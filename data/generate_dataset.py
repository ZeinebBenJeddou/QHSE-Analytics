"""
Générateur de dataset QHSE pour tests — 60 fichiers Excel variés
Couvre: propres, sales, partiels, mixtes, grandes/moyennes/petites tailles
"""

import random
import os
from pathlib import Path
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
import copy

random.seed(42)

OUTPUT_DIR = Path(__file__).parent.parent / "data"
OUTPUT_DIR.mkdir(exist_ok=True)

# ─── KPI MASTER DATA ──────────────────────────────────────────────────────────

KPIS = [
    # (nom_standard, categorie_code, valeur_n_exemple, valeur_n1_exemple, unite)
    # QUALITE
    ("Taux de Non-Conformité", "Q", 3.2, 2.8, "%"),
    ("Coût de la Non-Qualité", "Q", 1200, 900, "k€"),
    ("Taux de Satisfaction Client", "Q", 87.5, 84.0, "%"),
    ("Délai Moyen de Livraison", "Q", 4.2, 3.8, "jours"),
    ("Taux de Rebuts", "Q", 2.1, 1.7, "%"),
    ("First Pass Yield", "Q", 93.4, 91.0, "%"),
    ("Nombre de Réclamations Clients", "Q", 7, 5, "Nombre"),
    ("Taux de Réussite des Audits", "Q", 88.0, 82.0, "%"),
    # HYGIENE & SANTE
    ("Taux d'Absentéisme", "H", 4.8, 5.2, "%"),
    ("Taux de Maladies Professionnelles", "H", 0.8, 1.2, "cas/1000"),
    ("Conformité Ergonomique", "H", 91.0, 88.5, "%"),
    ("Taux de Visites Médicales", "H", 97.0, 95.0, "%"),
    ("Qualité de l'Air (CO2)", "H", 750, 820, "ppm"),
    ("Taux de Renouvellement d'Air", "H", 22.0, 20.0, "vol/h"),
    ("Indice d'Exposition au Bruit", "H", 81.0, 83.0, "dB(A)"),
    ("Usage des Équipements de Repos", "H", 65.0, 58.0, "%"),
    # SECURITE
    ("Taux de Fréquence (TF1)", "S", 9.2, 12.4, "acc/million h"),
    ("Taux de Gravité (TG)", "S", 0.7, 0.9, "j.perdus/1000 h"),
    ("Nombre de Presque-accidents", "S", 8, 11, "Nombre"),
    ("Heures de Formation Sécurité", "S", 14.5, 10.0, "h/an"),
    ("Taux de Port des EPI", "S", 96.2, 93.0, "%"),
    ("Nombre de Situations Dangereuses", "S", 18, 27, "Nombre"),
    ("Délai Levée des Non-Conformités", "S", 5.3, 8.1, "jours"),
    ("Nombre de Visites Sécurité (VMS)", "S", 9, 7, "Nombre"),
    # ENVIRONNEMENT
    ("Consommation Électricité", "E", 180, 210, "KWh/t"),
    ("Consommation Eau", "E", 120, 145, "m³"),
    ("Taux de Valorisation Déchets", "E", 72.0, 65.0, "%"),
    ("Émissions CO2 (Scope 1&2)", "E", 3200, 3800, "tCO2eq"),
    ("Volume Déchets Dangereux", "E", 140, 175, "kg"),
    ("Consommation de Papier", "E", 2.1, 2.8, "rames"),
    ("Incidents Environnementaux", "E", 1, 2, "Nombre"),
    ("Part Énergie Renouvelable", "E", 28.0, 18.0, "%"),
]

# ─── VARIANTES DE NOMS DE COLONNES ────────────────────────────────────────────

KPI_COL_VARIANTS = [
    "KPI", "Indicateur", "Libelle", "Libellé", "Intitulé", "Nom KPI",
    "Nom de l'indicateur", "Description", "INDICATEUR", "kpi", "Indicateurs QHSE",
    "Paramètre", "Item", "Metric", "Indicator Name",
]

N_COL_VARIANTS = [
    "Valeur N", "N", "Actuel", "Courant", "2024", "2025",
    "En cours", "Période N", "Valeur Actuelle", "N (2024)", "Année N",
    "Current", "Mesure N", "Réalisé N", "Val. N", "Exercice N",
]

N1_COL_VARIANTS = [
    "N-1", "N1", "Valeur N-1", "Valeur N1", "Précédent", "Antérieur",
    "2023", "2022", "Période N-1", "Année N-1", "N-1 (2023)", "Previous",
    "Mesure N-1", "Réalisé N-1", "Val. N-1", "Exercice N-1",
]

CAT_COL_VARIANTS = [
    "Catégorie", "Domaine", "Thème", "Axe", "Famille", "Type",
    "Rubrique", "Pilier", "Category", "Domaine QHSE",
]

UNIT_COL_VARIANTS = [
    "Unité", "Unite", "UdM", "Mesure", "Unit", "Unité de mesure",
]

COMMENT_COL_VARIANTS = [
    "Commentaire", "Observation", "Note", "Remarque", "Actions", "Plan d'action",
]

# ─── VARIANTES DE NOMS DE KPI (pour la saleté) ────────────────────────────────

KPI_DIRTY_VARIANTS = {
    "Taux de Non-Conformité": [
        "Taux NC", "NC (%)", "Non conformité", "non-conformité", "Tx Non Conformité",
        "Taux de non conformite", "% NC", "TNC", "Non-Conformities Rate",
    ],
    "Coût de la Non-Qualité": [
        "Cout NQ", "CNQ", "Non-qualité (k€)", "Coût non qualité", "CNQ (k€)",
        "Coût NQ", "Coût de NQ",
    ],
    "Taux de Satisfaction Client": [
        "Satisfaction client (%)", "TSC", "Sat. Client", "Customer Satisfaction",
        "Satisfaction clients", "Taux satisfaction", "NPS approximatif",
    ],
    "Taux de Fréquence (TF1)": [
        "TF1", "TF", "Taux Freq", "Fréquence accidents", "IF (acc freq)",
        "Tx Freq", "Taux de fréquence", "Accident Rate",
    ],
    "Taux de Gravité (TG)": [
        "TG", "Gravité", "Severity Rate", "Taux gravité", "TGrav",
        "Tx Gravité", "Indice de gravité",
    ],
    "Taux de Port des EPI": [
        "Port EPI (%)", "EPI", "PPE compliance", "Port des EPI",
        "Taux EPI", "Conformité EPI",
    ],
    "Taux d'Absentéisme": [
        "Absentéisme", "Absent.", "Tx Absentéisme", "Absenteisme (%)",
        "Taux absence", "Absenteeism Rate", "TA",
    ],
    "Consommation Électricité": [
        "Elec (KWh/t)", "Electricité", "Conso Elec", "KWh consommés",
        "Energie élec", "Consommation elec", "Elec. Consumption",
    ],
    "Émissions CO2 (Scope 1&2)": [
        "CO2", "Emis. CO2", "GES", "Carbone (tCO2)", "Carbon emissions",
        "CO2 scope 1+2", "Émissions GES",
    ],
    "Taux de Valorisation Déchets": [
        "Valorisation déchets", "Recycling rate", "Déchets valorisés (%)",
        "Taux recyclage", "Valorisation (%)", "% déchets valorisés",
    ],
}

def dirty_kpi_name(nom):
    """Retourne une variante sale du nom KPI si disponible, sinon une variation mineure."""
    if nom in KPI_DIRTY_VARIANTS and random.random() < 0.7:
        return random.choice(KPI_DIRTY_VARIANTS[nom])
    # Variations mineures
    variations = [
        nom,
        nom.lower(),
        nom.upper(),
        nom.replace("é", "e").replace("è", "e").replace("ê", "e"),
        nom + " ",  # espace trailing
        " " + nom,  # espace leading
        nom + " (objectif)",
        nom.replace("-", " "),
    ]
    return random.choice(variations)

def dirty_value(val, prob=0.25):
    """Introduit aléatoirement des valeurs sales."""
    r = random.random()
    if r < prob * 0.15:
        return None  # valeur manquante
    if r < prob * 0.25:
        return ""  # vide
    if r < prob * 0.30:
        return "N/A"
    if r < prob * 0.35:
        return "nd"
    if r < prob * 0.40:
        return f"{val}%"  # unité collée
    if r < prob * 0.45:
        return str(val).replace(".", ",")  # virgule décimale
    if r < prob * 0.50:
        return f"≈ {val}"  # approximation
    if r < prob * 0.55:
        return val * random.uniform(0.5, 2.5)  # valeur aberrante
    if r < prob * 0.60:
        return -abs(val)  # valeur négative aberrante
    return val

def pick_periods():
    """Choisit aléatoirement les années N et N-1."""
    n = random.choice([2024, 2025])
    return n, n - 1

def make_col_headers(include_category=True, include_unit=False, include_comment=False):
    """Génère une combinaison aléatoire d'en-têtes de colonnes."""
    kpi_col = random.choice(KPI_COL_VARIANTS)
    n_col = random.choice(N_COL_VARIANTS)
    n1_col = random.choice(N1_COL_VARIANTS)

    cols = [kpi_col, n_col, n1_col]
    col_roles = ["kpi", "n", "n1"]

    # Ordre aléatoire de N et N-1
    if random.random() < 0.4:
        cols[1], cols[2] = cols[2], cols[1]
        col_roles[1], col_roles[2] = col_roles[2], col_roles[1]

    if include_category and random.random() < 0.5:
        pos = random.randint(0, len(cols))
        cols.insert(pos, random.choice(CAT_COL_VARIANTS))
        col_roles.insert(pos, "cat")

    if include_unit and random.random() < 0.35:
        pos = random.randint(1, len(cols))
        cols.insert(pos, random.choice(UNIT_COL_VARIANTS))
        col_roles.insert(pos, "unit")

    if include_comment and random.random() < 0.45:
        cols.append(random.choice(COMMENT_COL_VARIANTS))
        col_roles.append("comment")

    return cols, col_roles

def select_kpis(size_category):
    """Sélectionne un sous-ensemble de KPIs selon la taille."""
    if size_category == "small":
        n = random.randint(3, 8)
    elif size_category == "medium":
        n = random.randint(10, 20)
    else:  # large
        n = random.randint(25, 32)
    return random.sample(KPIS, min(n, len(KPIS)))

def get_cell_value(role, kpi_row, dirty_prob, year_n, year_n1):
    """Retourne la valeur d'une cellule selon son rôle."""
    nom, cat, val_n, val_n1, unite = kpi_row
    if role == "kpi":
        return nom
    elif role == "n":
        return dirty_value(val_n * random.uniform(0.8, 1.2), dirty_prob)
    elif role == "n1":
        return dirty_value(val_n1 * random.uniform(0.85, 1.15), dirty_prob)
    elif role == "cat":
        return cat if random.random() > 0.1 else None
    elif role == "unit":
        return unite
    elif role == "comment":
        comments = [
            "À surveiller", "Objectif atteint", "Plan d'action en cours",
            "Voir rapport détaillé", "", None, "Action corrective #" + str(random.randint(100, 999)),
            "Non renseigné", "Données provisoires",
        ]
        return random.choice(comments)
    return None

def add_noise_rows(ws, current_row, dirty_prob):
    """Ajoute des lignes parasites (vides, commentaires, totaux...)."""
    noise_type = random.random()
    if noise_type < 0.3:
        # Ligne complètement vide
        ws.append([None] * ws.max_column)
        return current_row + 1
    elif noise_type < 0.5:
        # Ligne de commentaire/titre parasite
        ws.cell(row=current_row, column=1, value=random.choice([
            "Source: Reporting mensuel", "* Données provisoires",
            "Total", "Sous-total", "NB: valeurs en cours de validation",
            "Mise à jour: " + str(random.randint(1, 28)) + "/0" + str(random.randint(1, 9)) + "/2024",
        ]))
        return current_row + 1
    elif noise_type < 0.65 and dirty_prob > 0.2:
        # Ligne dupliquée d'une ligne précédente
        if ws.max_row > 3:
            src_row = random.randint(3, ws.max_row)
            for col in range(1, ws.max_column + 1):
                ws.cell(row=current_row, column=col,
                        value=ws.cell(row=src_row, column=col).value)
        return current_row + 1
    return current_row

def apply_style_noise(ws, dirty_prob):
    """Applique des variations de style pour simuler des fichiers réels."""
    if random.random() < 0.5:
        # Couleur d'en-tête aléatoire
        colors = ["4472C4", "ED7D31", "A9D18E", "FF0000", "70AD47", "5B9BD5", "FFC000"]
        fill = PatternFill("solid", fgColor=random.choice(colors))
        for cell in ws[1]:
            if cell.value:
                cell.fill = fill
                cell.font = Font(bold=True, color="FFFFFF")

    if random.random() < 0.3:
        # Freeze panes
        ws.freeze_panes = "B2"

    if random.random() < 0.4:
        # Ajustement largeur colonnes
        for col in ws.columns:
            max_len = max((len(str(c.value or "")) for c in col), default=10)
            ws.column_dimensions[get_column_letter(col[0].column)].width = min(max_len + 4, 40)

def generate_file(filepath, size_category, dirty_prob, profile):
    """
    Génère un fichier Excel QHSE.
    profile: dict avec des options de génération
    """
    wb = openpyxl.Workbook()
    ws = wb.active

    kpis_selected = select_kpis(size_category)
    year_n, year_n1 = pick_periods()

    # Nom de l'onglet
    sheet_names = ["KPIs", "Indicateurs", "Données QHSE", "Tableau de bord",
                   "Reporting", "Feuil1", "Sheet1", "QHSE", "Bilan"]
    ws.title = random.choice(sheet_names)

    # Lignes parasites en haut (certains fichiers)
    header_start_row = 1
    if profile.get("preamble") and dirty_prob > 0.15:
        preamble_lines = random.randint(1, 4)
        preamble_texts = [
            ["Rapport QHSE mensuel", None, None],
            ["Société: ACME Industries", "Période: " + str(year_n), None],
            ["Responsable: M. Dupont", "Date: 15/01/" + str(year_n), None],
            [None, None, None],
            ["", "", ""],
        ]
        for line in random.sample(preamble_texts, min(preamble_lines, len(preamble_texts))):
            ws.append(line)
            header_start_row += 1

    # Colonnes
    col_headers, col_roles = make_col_headers(
        include_category=profile.get("include_cat", True),
        include_unit=profile.get("include_unit", False),
        include_comment=profile.get("include_comment", False),
    )

    # Colonnes fantômes (colonnes parasites vides ou avec données sans rapport)
    ghost_cols = []
    if dirty_prob > 0.2 and random.random() < 0.4:
        ghost_count = random.randint(1, 3)
        ghost_names = ["Responsable", "Statut", "Date MAJ", "Objectif", "Écart",
                       "Tendance", "Actions", "Budget", "Poids", "Ref.", ""]
        ghost_cols = random.sample(ghost_names, ghost_count)
        insert_pos = random.randint(1, len(col_headers))
        for g in ghost_cols:
            col_headers.insert(insert_pos, g)
            col_roles.insert(insert_pos, "ghost")
            insert_pos += 1

    # Ligne d'en-tête
    header_row = []
    for h in col_headers:
        if dirty_prob > 0.1 and random.random() < 0.1:
            header_row.append(h.lower() if h else h)
        elif dirty_prob > 0.1 and random.random() < 0.05:
            header_row.append(h.upper() if h else h)
        else:
            header_row.append(h)
    ws.append(header_row)

    # Double en-tête (some files have two header rows)
    if profile.get("double_header") and random.random() < 0.3:
        sub_headers = []
        for role in col_roles:
            if role == "n":
                sub_headers.append("(" + str(year_n) + ")")
            elif role == "n1":
                sub_headers.append("(" + str(year_n1) + ")")
            else:
                sub_headers.append(None)
        ws.append(sub_headers)

    # Lignes de données
    current_row = ws.max_row + 1
    for kpi_row in kpis_selected:
        nom, cat, val_n, val_n1, unite = kpi_row

        # Nom du KPI (sale ou propre)
        kpi_display = dirty_kpi_name(nom) if dirty_prob > 0.1 else nom

        row_data = []
        for role in col_roles:
            if role == "kpi":
                row_data.append(kpi_display)
            elif role == "ghost":
                ghost_vals = [None, None, None, "OK", "NOK", "En cours",
                              str(random.randint(1, 100)) + "%", None]
                row_data.append(random.choice(ghost_vals) if random.random() < 0.5 else None)
            else:
                row_data.append(get_cell_value(role, kpi_row, dirty_prob, year_n, year_n1))

        ws.append(row_data)
        current_row += 1

        # Injection de lignes parasites
        if dirty_prob > 0.15 and random.random() < dirty_prob * 0.4:
            current_row = add_noise_rows(ws, current_row, dirty_prob)
            current_row += 1

    # Lignes supplémentaires vides à la fin (fichiers réels souvent)
    if random.random() < 0.5:
        for _ in range(random.randint(1, 5)):
            ws.append([None] * len(col_headers))

    # Style
    apply_style_noise(ws, dirty_prob)

    # Onglet supplémentaire dans certains fichiers
    if profile.get("multi_sheet") and random.random() < 0.4:
        ws2 = wb.create_sheet(title=random.choice(["Résumé", "Charts", "Raw Data", "Archive", "Feuil2"]))
        ws2.append(["Ce tableau est vide ou non pertinent"])

    wb.save(filepath)

# ─── CATALOGUE DE FICHIERS À GÉNÉRER ──────────────────────────────────────────

FILE_SPECS = [
    # (nom_fichier, taille, dirty_prob, profil)

    # === FICHIERS PROPRES (baseline) ===
    ("QHSE_clean_template_Q1_2024.xlsx",      "small",  0.00, {"include_cat": True}),
    ("QHSE_clean_full_2024.xlsx",             "large",  0.00, {"include_cat": True, "include_unit": True}),
    ("QHSE_clean_medium_2024.xlsx",           "medium", 0.00, {"include_cat": True}),
    ("QHSE_propre_securite_2024.xlsx",        "small",  0.02, {"include_cat": False}),
    ("QHSE_propre_environnement_2024.xlsx",   "small",  0.02, {"include_comment": True}),

    # === PETITS FICHIERS SALES ===
    ("rapport_kpi_janv24_brut.xlsx",          "small",  0.30, {"preamble": True}),
    ("suivi_indicateurs_fev24.xlsx",          "small",  0.35, {"double_header": True}),
    ("tableau_bord_mars24.xlsx",              "small",  0.40, {"include_cat": True, "preamble": True}),
    ("kpis_site_A_2024.xlsx",                 "small",  0.45, {"include_unit": True, "double_header": True}),
    ("extrait_reporting_Q2_2024.xlsx",        "small",  0.25, {"include_comment": True}),
    ("fiche_suivi_accidents_2024.xlsx",       "small",  0.55, {"preamble": True, "multi_sheet": True}),
    ("bilan_mensuel_mai24.xlsx",              "small",  0.35, {"include_cat": True}),
    ("donnees_brutes_juin24.xlsx",            "small",  0.50, {"preamble": True, "include_unit": True}),
    ("kpis_hyg_sante_S1.xlsx",               "small",  0.30, {"double_header": True}),
    ("export_ERP_qualite_juil24.xlsx",        "small",  0.45, {"multi_sheet": True, "preamble": True}),
    ("saisie_manuelle_aout24.xlsx",           "small",  0.60, {"preamble": True, "include_comment": True}),
    ("tableau_NC_sept24.xlsx",                "small",  0.40, {"include_cat": True, "double_header": True}),
    ("reporting_hebdo_oct24.xlsx",            "small",  0.35, {"preamble": True}),
    ("bilan_trim_T3_2024.xlsx",              "small",  0.25, {"include_unit": True, "include_comment": True}),
    ("suivi_EPI_nov24.xlsx",                  "small",  0.50, {"preamble": True, "multi_sheet": True}),

    # === FICHIERS MOYENS SALES ===
    ("rapport_annuel_2023_QHSE.xlsx",        "medium", 0.25, {"include_cat": True, "preamble": True}),
    ("bilan_complet_S1_2024.xlsx",           "medium", 0.35, {"double_header": True, "include_unit": True}),
    ("consolidation_sites_2024.xlsx",        "medium", 0.30, {"include_cat": True, "multi_sheet": True}),
    ("export_SIRH_absenteisme_2024.xlsx",    "medium", 0.40, {"preamble": True, "include_comment": True}),
    ("donnees_securite_2024_consolide.xlsx", "medium", 0.45, {"multi_sheet": True}),
    ("KPIs_environnement_2023_2024.xlsx",    "medium", 0.20, {"include_cat": True, "include_unit": True}),
    ("tableau_suivi_mensuel_2024.xlsx",      "medium", 0.50, {"preamble": True, "double_header": True}),
    ("reporting_qualite_clients_T4.xlsx",   "medium", 0.35, {"include_comment": True}),
    ("suivi_formation_securite_2024.xlsx",  "medium", 0.40, {"multi_sheet": True, "preamble": True}),
    ("indicateurs_RH_sante_2024.xlsx",      "medium", 0.30, {"include_cat": True}),
    ("bilan_dechets_et_energie_2024.xlsx",  "medium", 0.45, {"include_unit": True, "include_comment": True}),
    ("export_SAP_qhse_janv_juin24.xlsx",    "medium", 0.55, {"preamble": True, "multi_sheet": True}),
    ("tableau_pilotage_QHSE_2024.xlsx",     "medium", 0.20, {"include_cat": True, "include_unit": True}),
    ("donnees_audit_interne_2024.xlsx",     "medium", 0.40, {"preamble": True, "double_header": True}),
    ("suivi_conformite_ISO45001.xlsx",      "medium", 0.35, {"include_cat": True, "include_comment": True}),
    ("rapport_CHSCT_2024_complet.xlsx",     "medium", 0.50, {"multi_sheet": True, "preamble": True}),

    # === GRANDS FICHIERS SALES ===
    ("consolidation_annuelle_2023_2024.xlsx",    "large",  0.20, {"include_cat": True, "include_unit": True, "preamble": True}),
    ("reporting_global_QHSE_2024.xlsx",          "large",  0.30, {"include_cat": True, "multi_sheet": True}),
    ("bilan_groupe_multisite_2024.xlsx",         "large",  0.35, {"preamble": True, "double_header": True, "include_comment": True}),
    ("export_complet_tous_indicateurs.xlsx",     "large",  0.15, {"include_cat": True, "include_unit": True}),
    ("tableau_de_bord_direction_2024.xlsx",      "large",  0.25, {"include_cat": True, "include_comment": True}),
    ("donnees_brutes_systeme_GMAO.xlsx",         "large",  0.50, {"preamble": True, "multi_sheet": True}),
    ("rapport_ISO14001_ISO45001_2024.xlsx",      "large",  0.30, {"include_cat": True, "include_unit": True, "double_header": True}),
    ("bilan_QHSE_annuel_direction.xlsx",         "large",  0.20, {"preamble": True, "include_comment": True}),
    ("data_lake_export_qhse_raw.xlsx",           "large",  0.60, {"preamble": True, "multi_sheet": True, "include_unit": True}),
    ("synthese_multisite_complet_2024.xlsx",     "large",  0.40, {"include_cat": True, "preamble": True, "double_header": True}),

    # === FICHIERS TRÈS SALES / CAS EXTRÊMES ===
    ("copie_fichier_excel_ancien.xlsx",          "small",  0.75, {"preamble": True, "double_header": True, "multi_sheet": True}),
    ("brouillon_kpis_non_valide.xlsx",           "medium", 0.80, {"preamble": True, "include_unit": True, "include_comment": True}),
    ("export_csv_converti_excel.xlsx",           "medium", 0.70, {"preamble": True}),
    ("fichier_incomplet_colonnes_manquantes.xlsx","small", 0.85, {"include_cat": False, "double_header": False}),
    ("données_mixtes_multiformats.xlsx",          "medium", 0.75, {"preamble": True, "multi_sheet": True, "double_header": True}),

    # === FORMATS COLONNES ALTERNATIFS (tests header detection) ===
    ("kpis_avec_annees_en_colonnes.xlsx",        "medium", 0.15, {"include_cat": True}),
    ("indicateurs_format_anglais.xlsx",          "small",  0.10, {"include_cat": True}),
    ("tableau_sans_categorie.xlsx",              "medium", 0.20, {"include_cat": False}),
    ("rapport_avec_unites.xlsx",                 "medium", 0.15, {"include_unit": True, "include_cat": True}),
    ("format_double_entete.xlsx",                "medium", 0.10, {"double_header": True, "include_cat": True}),
    ("bilan_avec_commentaires.xlsx",             "medium", 0.20, {"include_comment": True, "include_cat": True}),

    # === FICHIERS BONUS (divers) ===
    ("QHSE_site_nord_2024.xlsx",                 "small",  0.30, {"preamble": True}),
    ("QHSE_site_sud_2024.xlsx",                  "small",  0.30, {"preamble": True}),
    ("QHSE_site_est_2024.xlsx",                  "medium", 0.25, {"include_cat": True}),
    ("QHSE_groupe_Q4_2024.xlsx",                 "medium", 0.35, {"include_comment": True}),
    ("bilan_prev_2025.xlsx",                     "small",  0.20, {"include_cat": True}),
]

# ─── GÉNÉRATION ───────────────────────────────────────────────────────────────

def main():
    print(f"Génération de {len(FILE_SPECS)} fichiers dans {OUTPUT_DIR} ...\n")

    for i, (filename, size, dirty_prob, profile) in enumerate(FILE_SPECS, 1):
        filepath = OUTPUT_DIR / filename
        generate_file(filepath, size, dirty_prob, profile)
        dirt_label = (
            "PROPRE" if dirty_prob < 0.05 else
            "LÉGER"  if dirty_prob < 0.30 else
            "SALE"   if dirty_prob < 0.55 else
            "TRÈS SALE"
        )
        print(f"[{i:02d}/{len(FILE_SPECS)}] {filename:<55} taille={size:<6} qualité={dirt_label}")

    print(f"\nTerminé. {len(FILE_SPECS)} fichiers générés dans {OUTPUT_DIR}")

if __name__ == "__main__":
    main()
