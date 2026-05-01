import pandas as pd
import random
import os
from faker import Faker

fake = Faker()

# 📁 dossier de sortie
OUTPUT_DIR = "test_qhse_files"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 🔁 variantes de noms KPI
KPI_VARIANTS = {
    "Nombre accidents": [
        "Nb accidents", "nombre_accidents", "Accidents", "ACCIDENT_COUNT", "nbr accidents"
    ],
    "Taux conformité": [
        "taux_conformite", "compliance_rate", "Taux conformité (%)", "tx conf"
    ],
    "Non conformités": [
        "NC", "non_conformites", "nonConformité", "NC_count"
    ],
    "Incidents sécurité": [
        "incidents", "security incidents", "incidents_securite"
    ]
}

# 📊 catégories variantes
CATEGORY_VARIANTS = [
    "Sécurité", "SECURITE", "securite",
    "Qualité", "qualite", "QUALITY",
    "Environnement", "env", "ENVIRONNEMENT"
]

# 📏 unités possibles (avec incohérences)
UNIT_VARIANTS = [
    "%", "pourcentage", "ratio",
    "nombre", "count", "nb",
    "jours", "heures",
    "", None  # valeurs manquantes
]

# 🧪 champs supplémentaires
EXTRA_COLUMNS = [
    "commentaire", "site", "responsable", "date_saisie",
    "note_interne", "code_projet"
]


def random_kpi_name():
    base = random.choice(list(KPI_VARIANTS.keys()))
    return random.choice(KPI_VARIANTS[base])


def random_unit():
    return random.choice(UNIT_VARIANTS)


def generate_value():
    # 🔥 valeurs normales / aberrantes / texte
    r = random.random()

    if r < 0.1:
        return None
    elif r < 0.15:
        return -999  # incohérent
    elif r < 0.2:
        return "N/A"  # texte
    else:
        return round(random.uniform(0, 100), 2)


def generate_dataframe(num_rows):
    data = []

    for _ in range(num_rows):
        row = {
            "kpi_name": random_kpi_name(),
            "categorie": random.choice(CATEGORY_VARIANTS),
            "valeur_N": generate_value(),
            "valeur_N_1": generate_value(),
            "unite": random_unit()
        }

        # 🧪 bruit
        for col in random.sample(EXTRA_COLUMNS, random.randint(0, len(EXTRA_COLUMNS))):
            row[col] = fake.word()

        data.append(row)

    df = pd.DataFrame(data)

    # 🔀 désordre colonnes
    cols = list(df.columns)
    random.shuffle(cols)
    df = df[cols]

    return df


def generate_files(num_files=25):
    for i in range(num_files):

        size_type = random.choice(["small", "medium", "large"])

        if size_type == "small":
            rows = random.randint(5, 20)
        elif size_type == "medium":
            rows = random.randint(50, 150)
        else:
            rows = random.randint(300, 1000)

        df = generate_dataframe(rows)

        filename = f"{OUTPUT_DIR}/qhsedata_{i+1}_{size_type}.xlsx"
        df.to_excel(filename, index=False)

        print(f"✔ Fichier généré: {filename} ({rows} lignes)")


if __name__ == "__main__":
    generate_files(30)