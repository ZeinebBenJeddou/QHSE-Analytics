import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-auth-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatDividerModule,
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {

  bars = [
    { prev: 38, curr: 28 },
    { prev: 45, curr: 32 },
    { prev: 30, curr: 22 },
    { prev: 52, curr: 40 },
    { prev: 41, curr: 30 },
    { prev: 35, curr: 18 },
  ];

  stats = [
    { value: '4',    label: 'Catégories QHSE couvertes' },
    { value: '16+',  label: 'Indicateurs de performance' },
    { value: '3',    label: 'Niveaux de criticité' },
    { value: '100%', label: 'Automatisé par IA' },
  ];
}