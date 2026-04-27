import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { ImportService } from '../../../../core/services/import.service';
import { AiAnalysisService } from '../../../../core/services/ai-analysis.service';
import { MappingConfigRequest, MappingTemplateResponse } from '../../../../core/models/analyse-ia.model';

interface ColumnMapping {
  excelColumn: string;
  kpiId: number | null;
}

@Component({
  selector: 'app-mapping',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatChipsModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTooltipModule, MatDividerModule,
  ],
  templateUrl: './mapping.component.html',
  styleUrls: ['./mapping.component.css'],
})
export class MappingComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private aiService = inject(AiAnalysisService);
  private snackBar = inject(MatSnackBar);

  sessionId = signal<number | null>(null);
  loading = signal(false);
  saving = signal(false);

  /** Detected columns from the uploaded file */
  detectedColumns = signal<string[]>([]);

  /** User-defined mappings */
  mappings = signal<ColumnMapping[]>([]);

  /** Saved templates */
  templates = signal<MappingTemplateResponse[]>([]);

  templateName = '';

  // Predefined common column suggestions
  knownColumns = [
    'Date Accident', 'Type NC', 'Catégorie', 'Nombre Incidents',
    'Taux Fréquence', 'Taux Gravité', 'Nombre AC', 'Département',
  ];

  ngOnInit() {
    const sid = this.route.snapshot.queryParamMap.get('sessionId');
    if (sid) this.sessionId.set(+sid);
    this.loadTemplates();
    this.initDefaultMappings();
  }

  initDefaultMappings() {
    // Initialize with common columns as a starting point if no columns detected
    this.mappings.set(
      this.knownColumns.map(col => ({ excelColumn: col, kpiId: null }))
    );
  }

  loadTemplates() {
    this.aiService.getMappingTemplates().subscribe({
      next: res => this.templates.set(res),
      error: () => {},
    });
  }

  addRow() {
    this.mappings.update(m => [...m, { excelColumn: '', kpiId: null }]);
  }

  removeRow(index: number) {
    this.mappings.update(m => m.filter((_, i) => i !== index));
  }

  updateColumn(index: number, value: string) {
    this.mappings.update(m => m.map((item, i) => i === index ? { ...item, excelColumn: value } : item));
  }

  updateKpi(index: number, kpiId: number) {
    this.mappings.update(m => m.map((item, i) => i === index ? { ...item, kpiId } : item));
  }

  saveTemplate() {
    if (!this.templateName.trim()) {
      this.snackBar.open('Veuillez saisir un nom de template.', 'OK', { duration: 3000 });
      return;
    }
    const validMappings: MappingConfigRequest[] = this.mappings()
      .filter(m => m.excelColumn && m.kpiId)
      .map(m => ({ excelColumn: m.excelColumn, kpiId: m.kpiId! }));

    if (!validMappings.length) {
      this.snackBar.open('Aucun mapping valide à sauvegarder.', 'OK', { duration: 3000 });
      return;
    }

    this.saving.set(true);
    this.aiService.saveMappingTemplate({ templateName: this.templateName, mappings: validMappings }).subscribe({
      next: res => {
        this.templates.update(t => [res, ...t]);
        this.templateName = '';
        this.saving.set(false);
        this.snackBar.open('Template sauvegardé !', 'OK', { duration: 3000 });
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open("Erreur lors de la sauvegarde.", 'OK', { duration: 3000 });
      }
    });
  }

  loadTemplate(t: MappingTemplateResponse) {
    this.mappings.set(t.mappings.map(m => ({ excelColumn: m.excelColumn, kpiId: m.kpiId })));
    this.snackBar.open(`Template "${t.templateName}" chargé.`, 'OK', { duration: 2000 });
  }

  deleteTemplate(id: number) {
    this.aiService.deleteMappingTemplate(id).subscribe({
      next: () => {
        this.templates.update(t => t.filter(x => x.id !== id));
        this.snackBar.open('Template supprimé.', 'OK', { duration: 2000 });
      }
    });
  }

  goBack() { this.router.navigate(['/analyste/import']); }
}
