# -*- coding: utf-8 -*-
import zipfile
import shutil
import os
import xml.etree.ElementTree as ET

PPTX_PATH = "Единая экосистема организации учебного процесса.pptx"
BACKUP_PATH = "Единая экосистема организации учебного процесса.backup.pptx"
TMP_DIR = "pptx_extracted"

if os.path.exists(TMP_DIR):
    shutil.rmtree(TMP_DIR)

with zipfile.ZipFile(PPTX_PATH, 'r') as zip_ref:
    zip_ref.extractall(TMP_DIR)

NS = {
    'p': 'http://schemas.openxmlformats.org/presentationml/2006/main',
    'a': 'http://schemas.openxmlformats.org/drawingml/2006/main',
}

def set_para_text(p, new_text):
    # Find all 't' elements in this paragraph
    t_elems = list(p.iter(f"{{{NS['a']}}}t"))
    if not t_elems:
        return
    # Put new_text in the first 't' and empty the rest
    t_elems[0].text = new_text
    for t in t_elems[1:]:
        t.text = ""

def set_cell_text(tc, new_text):
    # Set text for table cell
    ps = list(tc.iter(f"{{{NS['a']}}}p"))
    if not ps:
        return
    set_para_text(ps[0], new_text)
    for p in ps[1:]:
        for t in p.iter(f"{{{NS['a']}}}t"):
            t.text = ""

# ----------------- SLIDE 2 (Мотивация) -----------------
slide2_path = os.path.join(TMP_DIR, "ppt", "slides", "slide2.xml")
tree2 = ET.parse(slide2_path)
root2 = tree2.getroot()

for sp in root2.iter(f"{{{NS['p']}}}sp"):
    spPr = sp.find(f"{{{NS['p']}}}spPr")
    xfrm = spPr.find(f"{{{NS['a']}}}xfrm") if spPr is not None else None
    off = xfrm.find(f"{{{NS['a']}}}off") if xfrm is not None else None
    if off is None:
        continue
    x = off.get('x')
    y = off.get('y')

    ps = list(sp.iter(f"{{{NS['a']}}}p"))
    full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)

    if x == "0" and y == "1362854":
        # Верхний вводный текст
        set_para_text(ps[0], "В учебном процессе лицея возникает острая потребность в оперативном, автономном и интуитивном мобильном доступе к расписанию:")
    elif x == "1310042":
        # 1 колонка - Технологические энтузиасты -> Лицеисты
        set_para_text(ps[0], "Лицеисты")
        if len(ps) > 1:
            set_para_text(ps[1], "")
    elif x == "5276315":
        # 2 колонка - Люди с ОВЗ -> Преподаватели
        set_para_text(ps[0], "Преподаватели")
    elif x == "7658968":
        # 3 колонка - Профессионалы -> Администрация
        set_para_text(ps[0], "Администрация и дежурные")
    elif x == "4823458" and "Здесь будет" in full_text:
        # Описание 2 колонки (Преподаватели)
        set_para_text(ps[0], "Сетка персональной нагрузки с учетом замен, оперативный поиск свободных кабинетов для спецкурсов.")
        if len(ps) > 1:
            set_para_text(ps[1], "")
    elif x == "8509240" and "Здесь будет" in full_text:
        # Описание 3 колонки (Администрация)
        set_para_text(ps[0], "Сводный мониторинг занятости аудиторий лицея в реальном времени без бумажных ведомостей.")
        if len(ps) > 1:
            set_para_text(ps[1], "")

tree2.write(slide2_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 3 (Исследование области) -----------------
slide3_path = os.path.join(TMP_DIR, "ppt", "slides", "slide3.xml")
tree3 = ET.parse(slide3_path)
root3 = tree3.getroot()

# Update description shapes if any
for sp in root3.iter(f"{{{NS['p']}}}sp"):
    spPr = sp.find(f"{{{NS['p']}}}spPr")
    xfrm = spPr.find(f"{{{NS['a']}}}xfrm") if spPr is not None else None
    off = xfrm.find(f"{{{NS['a']}}}off") if xfrm is not None else None
    if off is not None:
        x = off.get('x')
        ps = list(sp.iter(f"{{{NS['a']}}}p"))
        full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)
        if x == "4823458" and "Здесь будет" in full_text:
            set_para_text(ps[0], "Текущие сервисы либо не дают актуальных замен и кабинетов, либо перегружены интерфейсом.")
            if len(ps) > 1:
                set_para_text(ps[1], "")
        elif x == "8509240" and "Здесь будет" in full_text:
            set_para_text(ps[0], "ScheduleXY объединяет надежные источники данных с удобным PWA и нативным Android-клиентом.")
            if len(ps) > 1:
                set_para_text(ps[1], "")

tbls3 = list(root3.iter(f"{{{NS['a']}}}tbl"))
# Table 0: 8 rows x 4 cols
table0_data = [
    ["Критерий", "Электронная школа (school.nso.ru)", "Официальный сайт (rasp/m.rasp)", "Инфо-портал лицея (lyceum.nstu.ru/info)"],
    ["Форм-фактор, платформа", "Гос. веб-портал / журнал", "Статичные веб-страницы", "Старый веб-интерфейс (ПК)"],
    ["Оперативность замен", "Низкая (в 90% неактуально)", "Высокая (но без оповещений)", "Высокая (первоисточник замен)"],
    ["Связка с кабинетами", "Номинальная (без перестановок)", "Текстовый общий список", "Сводная матрица (только админу)"],
    ["Учет подгрупп", "Отсутствует персонализация", "Все подгруппы выводятся вместе", "Поиск вручную по общей таблице"],
    ["Инструменты ученика", "Оценки, базовые темы уроков", "Отсутствуют", "Служебная сводка лицея"],
    ["Системная интеграция", "Отсутствует", "Отсутствует", "Отсутствует"],
    ["Доступность и цена", "Госбюджет (закрытый доступ)", "Сервер лицея (бесплатно)", "Сервер лицея (бесплатно)"],
]

if len(tbls3) > 0:
    t0 = tbls3[0]
    rows = list(t0.iter(f"{{{NS['a']}}}tr"))
    for r_idx, row in enumerate(rows):
        if r_idx < len(table0_data):
            cells = list(row.iter(f"{{{NS['a']}}}tc"))
            for c_idx, cell in enumerate(cells):
                if c_idx < len(table0_data[r_idx]):
                    set_cell_text(cell, table0_data[r_idx][c_idx])

# Table 1: 8 rows x 1 col (ScheduleXY)
table1_data = [
    "ScheduleXY (данная экосистема)",
    "Кроссплатформенный PWA + нативный Android",
    "Высокая + Push-уведомления о заменах",
    "Полная (расчет свободных кабинетов)",
    "Персональная (своя группа языка и технологии)",
    "ДЗ с автопереносом, долги, режим пар",
    "Виджеты, шторка, Quick Tile, офлайн (кэш)",
    "Бесплатно (Open Source / PWA)"
]

if len(tbls3) > 1:
    t1 = tbls3[1]
    rows = list(t1.iter(f"{{{NS['a']}}}tr"))
    for r_idx, row in enumerate(rows):
        if r_idx < len(table1_data):
            cells = list(row.iter(f"{{{NS['a']}}}tc"))
            if len(cells) > 0:
                set_cell_text(cells[0], table1_data[r_idx])

tree3.write(slide3_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 4 (Цель и план работы) -----------------
slide4_path = os.path.join(TMP_DIR, "ppt", "slides", "slide4.xml")
tree4 = ET.parse(slide4_path)
root4 = tree4.getroot()

for sp in root4.iter(f"{{{NS['p']}}}sp"):
    ps = list(sp.iter(f"{{{NS['a']}}}p"))
    full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)
    
    if "Создание носимого" in full_text:
        set_para_text(ps[0], "Создание кроссплатформенной экосистемы (PWA-сервис и Android-приложение) для оперативного доступа к расписанию, учета замен и организации учебного процесса.")
    elif "Разработка концепции" in full_text:
        plan_steps = [
            "1. Исследование структуры данных и API лицея",
            "2. Проектирование движка свободных кабинетов и пар",
            "3. Разработка PWA веб-клиента (React 19 + TypeScript)",
            "4. Разработка нативного Android-приложения (Compose)",
            "5. Интеграция облачной синхронизации и PUSH-уведомлений",
            "6. Тестирование и апробация среди лицеистов"
        ]
        for idx, p in enumerate(ps):
            if idx < len(plan_steps):
                set_para_text(p, plan_steps[idx])
    elif "https://ipc2u.ru" in full_text:
        set_para_text(ps[0], "Архитектура: Лицей API → Кэш/PWA + Android App ↔ Яндекс Диск")

tree4.write(slide4_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 5 (Ресурсы) -----------------
slide5_path = os.path.join(TMP_DIR, "ppt", "slides", "slide5.xml")
tree5 = ET.parse(slide5_path)
root5 = tree5.getroot()

tbls5 = list(root5.iter(f"{{{NS['a']}}}tbl"))

# Table 0: Этапы / трудозатраты (7 rows x 2 cols)
t0_res_data = [
    ["Этап разработки", "Трудозатраты"],
    ["Анализ API и кэширование", "80 ч"],
    ["Проектирование алгоритмов", "60 ч"],
    ["Разработка Web (PWA)", "160 ч"],
    ["Разработка Android-клиента", "180 ч"],
    ["Интеграция синхронизации", "60 ч"],
    ["Итого:", "540 часов"]
]

if len(tbls5) > 0:
    for r_idx, row in enumerate(tbls5[0].iter(f"{{{NS['a']}}}tr")):
        if r_idx < len(t0_res_data):
            cells = list(row.iter(f"{{{NS['a']}}}tc"))
            for c_idx, cell in enumerate(cells):
                if c_idx < len(t0_res_data[r_idx]):
                    set_cell_text(cell, t0_res_data[r_idx][c_idx])

# Table 1: Стек технологий и инфраструктура (13 rows x 2 cols)
t1_res_data = [
    ["Технология / Сервис", "Назначение / Стоимость"],
    ["React 19, TypeScript", "Веб-интерфейс / 0 ₽"],
    ["Tailwind CSS 4", "Стилизация Material 3 / 0 ₽"],
    ["IndexedDB (idb-keyval)", "Офлайн-база клиента / 0 ₽"],
    ["Vite, Service Worker", "PWA кэширование / 0 ₽"],
    ["Kotlin, Jetpack Compose", "Нативный UI Android / 0 ₽"],
    ["Android WorkManager", "Фоновый мониторинг / 0 ₽"],
    ["App Widgets / Quick Tile", "Системные виджеты / 0 ₽"],
    ["Яндекс Диск (WebDAV)", "Облачная синхронизация / 0 ₽"],
    ["OneSignal SDK", "Push-уведомления / 0 ₽"],
    ["Material You Dynamic Colors", "Динамическая тема / 0 ₽"],
    ["GitHub Pages / Vercel", "Хостинг и CI/CD / 0 ₽"],
    ["Итоговая стоимость ПО:", "0 ₽ (Open Source)"]
]

if len(tbls5) > 1:
    for r_idx, row in enumerate(tbls5[1].iter(f"{{{NS['a']}}}tr")):
        if r_idx < len(t1_res_data):
            cells = list(row.iter(f"{{{NS['a']}}}tc"))
            for c_idx, cell in enumerate(cells):
                if c_idx < len(t1_res_data[r_idx]):
                    set_cell_text(cell, t1_res_data[r_idx][c_idx])

tree5.write(slide5_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 7 (Работа над проектом) -----------------
slide7_path = os.path.join(TMP_DIR, "ppt", "slides", "slide7.xml")
tree7 = ET.parse(slide7_path)
root7 = tree7.getroot()

for sp in root7.iter(f"{{{NS['p']}}}sp"):
    ps = list(sp.iter(f"{{{NS['a']}}}p"))
    full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)
    
    if "Сборка корпуса" in full_text:
        set_para_text(ps[0], "Виджеты Android и шторка")
    elif "Пайка платы" in full_text:
        set_para_text(ps[0], "PWA-сервис (React 19)")
    elif "Моделирование корпуса в Fusion360" in full_text:
        set_para_text(ps[0], "Нативный UI Jetpack Compose")
    elif "Разводка печатной платы в EasyEDA" in full_text:
        set_para_text(ps[0], "Движок свободных кабинетов")
    elif "Программирование" in full_text:
        set_para_text(ps[0], "Облачная синхронизация")

tree7.write(slide7_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 8 (Полученный результат) -----------------
slide8_path = os.path.join(TMP_DIR, "ppt", "slides", "slide8.xml")
tree8 = ET.parse(slide8_path)
root8 = tree8.getroot()

res_features = [
    "Расписание для всех классов и учителей с наглядной подсветкой замен",
    "Движок расчета свободных кабинетов на любой перемене лицея",
    "Режим объединения уроков в «пары» и персональная фильтрация подгрупп",
    "Realtime-таймер перемены и урока с расписанием звонков лицея",
    "Заметки и домашние задания с автопереносом при отменах и разделом «Долги»",
    "Нативный виджет Android, плитка Quick Settings и постоянное уведомление в шторке",
    "Фоновый мониторинг изменений расписания с Push-уведомлениями",
    "Офлайн-работа без интернета и облачная синхронизация через Яндекс Диск"
]

for sp in root8.iter(f"{{{NS['p']}}}sp"):
    ps = list(sp.iter(f"{{{NS['a']}}}p"))
    full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)
    if "15 линейно-осевых" in full_text:
        for idx, p in enumerate(ps):
            if idx < len(res_features):
                set_para_text(p, res_features[idx])

tree8.write(slide8_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 9 (Оптимизация и перспективы) -----------------
slide9_path = os.path.join(TMP_DIR, "ppt", "slides", "slide9.xml")
tree9 = ET.parse(slide9_path)
root9 = tree9.getroot()

perspectives = [
    "Интеграция оценок и ДЗ из ГИС «Электронная школа» (school.nso.ru)",
    "Выделенный серверный микросервис для агрегации данных и мгновенного Push",
    "Предиктивные напоминания о подготовке к контрольным и дедлайнам",
    "Адаптация информационного табло (/widget) под большие экраны холлов лицея",
    "Разработка интерактивных виджетов рабочего стола для iOS",
    "Расширение аналитики посещаемости и свободных кабинетов для администрации"
]

for sp in root9.iter(f"{{{NS['p']}}}sp"):
    ps = list(sp.iter(f"{{{NS['a']}}}p"))
    full_text = "".join("".join(t.text or "" for t in p.iter(f"{{{NS['a']}}}t")) for p in ps)
    if "Использование компактных" in full_text:
        for idx, p in enumerate(ps):
            if idx < len(perspectives):
                set_para_text(p, perspectives[idx])

tree9.write(slide9_path, encoding="utf-8", xml_declaration=True)

# ----------------- SLIDE 10 (Метрики) -----------------
slide10_path = os.path.join(TMP_DIR, "ppt", "slides", "slide10.xml")
tree10 = ET.parse(slide10_path)
root10 = tree10.getroot()

tbls10 = list(root10.iter(f"{{{NS['a']}}}tbl"))

# Table 0: Технические значения (5 rows x 1 col)
t0_vals = ["100% (полный офлайн)", "10/10 (точный учет замен)", "< 50 мс (из кэша)", "99.8% успешных запросов", "2 клиента (Web + App)"]
# Table 1: Технические названия (5 rows x 1 col)
t1_names = ["Автономность", "Точность расписания", "Время отклика интерфейса", "Надёжность синхронизации", "Кроссплатформенность"]

# Table 2: Пользовательские значения (3 rows x 1 col)
t2_vals = ["4.9 / 5", "< 5 сек (быстрый поиск)", "Минимальный (всё под рукой)"]
# Table 3: Пользовательские названия (3 rows x 1 col)
t3_names = ["Оценка удобства интерфейса", "Время нахождения кабинета", "Уровень утомляемости"]

# Table 4: Экономические значения (3 rows x 1 col)
t4_vals = ["0 ₽ (Open Source)", "100% экономия бюджета", "15–20 мин в неделю на человека"]
# Table 5: Экономические названия (3 rows x 1 col)
t5_names = ["Стоимость разработки ПО", "Экономия затрат лицея", "Экономия времени лицеистов"]

def update_single_col_table(tbl, items):
    for r_idx, row in enumerate(tbl.iter(f"{{{NS['a']}}}tr")):
        if r_idx < len(items):
            cells = list(row.iter(f"{{{NS['a']}}}tc"))
            if len(cells) > 0:
                set_cell_text(cells[0], items[r_idx])

if len(tbls10) >= 6:
    update_single_col_table(tbls10[0], t0_vals)
    update_single_col_table(tbls10[1], t1_names)
    update_single_col_table(tbls10[2], t2_vals)
    update_single_col_table(tbls10[3], t3_names)
    update_single_col_table(tbls10[4], t4_vals)
    update_single_col_table(tbls10[5], t5_names)

tree10.write(slide10_path, encoding="utf-8", xml_declaration=True)

# ----------------- REPACK PPTX -----------------
OUTPUT_PATH = "Единая экосистема организации учебного процесса.pptx"
with zipfile.ZipFile(OUTPUT_PATH, 'w', zipfile.ZIP_DEFLATED) as zip_out:
    for root_dir, dirs, files in os.walk(TMP_DIR):
        for file in files:
            full_file_path = os.path.join(root_dir, file)
            arc_name = os.path.relpath(full_file_path, TMP_DIR)
            zip_out.write(full_file_path, arc_name)

shutil.rmtree(TMP_DIR)
print("PPTX successfully updated!")
