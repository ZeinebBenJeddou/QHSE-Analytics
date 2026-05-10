import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AdminService } from '../../../../core/services/admin.service';
import { RagKnowledgeResponse, RagSearchTestResultItem } from '../../models/admin.models';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-rag-admin',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatTableModule, MatChipsModule,
    MatProgressSpinnerModule, MatSnackBarModule, MatTooltipModule, MatDialogModule,
  ],
  templateUrl: './rag-admin.component.html',
  styleUrls: ['./rag-admin.component.css'],
})
export class RagAdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly snackBar     = inject(MatSnackBar);
  private readonly dialog       = inject(MatDialog);
  private readonly fb           = inject(FormBuilder);
  private readonly route        = inject(ActivatedRoute);

  entries: RagKnowledgeResponse[] = [];
  loading  = false;
  saving   = false;
  searching = false;
  error    = '';

  editingEntry: RagKnowledgeResponse | null = null;

  searchResults: RagSearchTestResultItem[] | null = null;
  searchError = '';

  displayedColumns = ['kpiName', 'category', 'definition', 'embedding', 'updatedAt', 'actions'];

  readonly categories = ['Q', 'H', 'S', 'E'];

  entryForm = this.fb.group({
    kpiName:    ['', [Validators.required, Validators.maxLength(255)]],
    definition: [''],
    thresholds: [''],
    category:   [''],
  });

  searchForm = this.fb.group({
    query:     ['', Validators.required],
    topK:      [5],
    threshold: [0.5],
  });

  ngOnInit(): void {
    const resolved = this.route.snapshot.data['entries'] as RagKnowledgeResponse[] | null;
    if (resolved) {
      this.entries = resolved;
    } else {
      this.loadEntries();
    }
  }

  get totalWithEmbedding(): number {
    return this.entries.filter(e => e.hasEmbedding).length;
  }

  // ── CRUD ────────────────────────────────────────────────────────────────

  startEdit(entry: RagKnowledgeResponse): void {
    this.editingEntry = entry;
    this.entryForm.setValue({
      kpiName:    entry.kpiName,
      definition: entry.definition ?? '',
      thresholds: entry.thresholds ?? '',
      category:   entry.category ?? '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit(): void {
    this.editingEntry = null;
    this.entryForm.reset();
  }

  submitEntry(): void {
    if (this.entryForm.invalid) return;
    this.saving = true;
    const raw = this.entryForm.value;
    const payload = {
      kpiName:    raw.kpiName!.trim(),
      definition: raw.definition ?? '',
      thresholds: raw.thresholds?.trim() || null,
      category:   raw.category || null,
    };

    const op$ = this.editingEntry
      ? this.adminService.updateRagEntry(this.editingEntry.id, payload)
      : this.adminService.createRagEntry(payload);

    op$.subscribe({
      next: () => {
        const msg = this.editingEntry ? 'Entrée mise à jour.' : 'Entrée créée.';
        this.snackBar.open(msg, 'Fermer', { duration: 3000 });
        this.editingEntry = null;
        this.entryForm.reset();
        this.loadEntries();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Erreur lors de la sauvegarde.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
        this.saving = false;
      },
      complete: () => { this.saving = false; },
    });
  }

  deleteEntry(entry: RagKnowledgeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer l\'entrée RAG',
        message: `Supprimer définitivement "${entry.kpiName}" de la base de connaissances ?`,
        confirmLabel: 'Supprimer',
        danger: true,
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.adminService.deleteRagEntry(entry.id).subscribe({
        next: () => {
          this.snackBar.open('Entrée supprimée.', 'Fermer', { duration: 3000 });
          this.loadEntries();
        },
        error: () => this.snackBar.open('Impossible de supprimer.', 'Fermer', { duration: 3000 }),
      });
    });
  }

  // ── SEARCH TEST ─────────────────────────────────────────────────────────

  runSearch(): void {
    if (this.searchForm.invalid) return;
    this.searching = true;
    this.searchError = '';
    this.searchResults = null;
    const raw = this.searchForm.value;
    this.adminService.testRagSearch({
      query:     raw.query!,
      topK:      raw.topK ?? 5,
      threshold: raw.threshold ?? 0.5,
    }).subscribe({
      next:     (r) => { this.searchResults = r; },
      error:    () => { this.searchError = 'Erreur lors de la recherche.'; },
      complete: () => { this.searching = false; },
    });
  }

  clearSearch(): void {
    this.searchResults = null;
    this.searchForm.reset({ topK: 5, threshold: 0.5 });
  }

  // ── HELPERS ─────────────────────────────────────────────────────────────

  private loadEntries(): void {
    this.loading = true;
    this.adminService.getRagEntries().subscribe({
      next:     (r) => { this.entries = r; },
      error:    () => { this.error = 'Impossible de charger la base RAG.'; },
      complete: () => { this.loading = false; },
    });
  }

  categoryLabel(code: string | null): string {
    const map: Record<string, string> = { Q: 'Qualité', H: 'Hygiène', S: 'Sécurité', E: 'Environnement' };
    return code ? (map[code] ?? code) : '—';
  }

  truncate(text: string | null, max = 80): string {
    if (!text) return '—';
    return text.length > max ? text.substring(0, max) + '…' : text;
  }

  isValidJson(val: string | null): boolean {
    if (!val?.trim()) return true;
    try { JSON.parse(val); return true; } catch { return false; }
  }
}
