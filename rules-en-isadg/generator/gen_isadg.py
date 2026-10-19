# Generates rules-en-isadg from data tables (plan step 2b). Regenerate rather than hand-edit:
#   python generator/gen_isadg.py   (from any directory; writes the module this file lives in)
# The item types, the specifications, the filter rule, the type groups, the grid, the pinned
# filters and translations/cs.xml come from the tables below, so they cannot drift apart.
# Languages: the ISO 639-2 list of package-isaar-cpf (the same LNG_* codes, shared by code);
# their Czech names come from package-cz-base where it declares them.
import os
import re

GEN = os.path.dirname(os.path.abspath(__file__)).replace(os.sep, '/') + '/'
ROOT = os.path.dirname(GEN.rstrip('/')) + '/'
SRC = ROOT + 'src/'
RS = 'rul_rule_set/ISADG/'
ISAAR_ISO = os.path.normpath(ROOT + '../package-isaar-cpf/generator/iso639-2.txt')
CZ_BASE_SPECS = os.path.normpath(ROOT + '../package-cz-base/src/rul_item_spec.xml')

PACKAGE = 'ISADG'
VERSION = 2


def write(path, text):
    full = (SRC if not path.startswith('/') else ROOT) + path.lstrip('/')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'w', encoding='utf-8', newline='').write(text)


def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def q(codes):
    return ', '.join('"%s"' % c for c in codes)


# ---------------------------------------------------------------- module files
write('/pom.xml', '''<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <artifactId>elza</artifactId>
        <groupId>cz.tacr.elza</groupId>
        <version>3.0-SNAPSHOT</version>
    </parent>
    <artifactId>rules-en-isadg</artifactId>
    <name>Rules ISAD(G)</name>
    <packaging>pom</packaging>
    <build>
        <plugins>
            <!-- Disable default .jar generation -->
            <plugin>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <id>default-jar</id>
                        <phase>none</phase>
                    </execution>
                </executions>
            </plugin>
            <!-- Create own package -->
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <executions>
                    <execution>
                        <id>rules-en-isadg</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                        <configuration>
                            <descriptors>
                                <descriptor>zip-src.xml</descriptor>
                            </descriptors>
                            <appendAssemblyId>false</appendAssemblyId>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
''')
write('/zip-src.xml', '''<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3 http://maven.apache.org/xsd/assembly-1.1.3.xsd">
  <id>isadg</id>
  <formats>
    <format>zip</format>
  </formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <fileSets>
    <fileSet>
      <directory>${project.basedir}/src</directory>
      <outputDirectory>.</outputDirectory>
      <useDefaultExcludes>true</useDefaultExcludes>
    </fileSet>
  </fileSets>
</assembly>
''')

# ---------------------------------------------------------------- package.xml
write('package.xml', '''<package>
  <code>%s</code>
  <name>ISAD(G) archival description</name>
  <version>%d</version>
  <description>Archival description of fonds according to ISAD(G), in English; creators and access points are ISAAR(CPF) entities.</description>
  <language>en</language>
  <dependencies>
    <dependency code="ISAAR_CPF" min-version="2" />
  </dependencies>
</package>
''' % (PACKAGE, VERSION))

# ---------------------------------------------------------------- item types
# (code, data type, name, shortcut, description, uses specifications, structure type, classes of RECORD_REF)
AGENTS = ['PERSON_INDIVIDUAL', 'FAMILY', 'PARTY_GROUP']
ALL_CLASSES = AGENTS + ['GEO', 'TERM']
items = [
    # 3.1 Identity statement
    ('ISADG_LEVEL', 'ENUM', 'Level of description', 'Level', 'ISAD(G) 3.1.4: level of the unit of description', True, None, None),
    ('ISADG_REFERENCE_CODE', 'UNITID', 'Reference code', 'Ref. code', 'ISAD(G) 3.1.1: reference code of the unit, unique in the fonds', False, None, None),
    ('ISADG_OTHER_ID', 'STRING', 'Other identifier', 'Other id.', 'Former reference code, accession number or another identifier; the specification says which', True, None, None),
    ('ISADG_TITLE', 'STRING', 'Title', 'Title', 'ISAD(G) 3.1.2: title of the unit of description, formal or supplied', False, None, None),
    ('ISADG_DATE', 'UNITDATE', 'Date(s) of creation', 'Date', 'ISAD(G) 3.1.3: date or range of dates of creation of the unit', False, None, None),
    ('ISADG_DATE_BULK', 'UNITDATE', 'Bulk dates', 'Bulk', 'ISAD(G) 3.1.3: dates of the bulk of the unit when they differ from the dates of creation', False, None, None),
    ('ISADG_DATE_TEXT', 'STRING', 'Date(s) as text', 'Date text', 'ISAD(G) 3.1.3: the dates in words when the structured date cannot express them', False, None, None),
    ('ISADG_EXTENT', 'TEXT', 'Extent and medium', 'Extent', 'ISAD(G) 3.1.5: extent and medium of the unit as it is stated in the finding aid, e.g. "128 boxes (12.5 linear metres)"', False, None, None),
    ('ISADG_EXTENT_QUANTITY', 'DECIMAL', 'Quantity', 'Quantity', 'ISAD(G) 3.1.5: measured extent; the specification is the unit of measure', True, None, None),
    ('ISADG_MATERIAL_TYPE', 'ENUM', 'Material type', 'Material', 'ISAD(G) 3.1.5: form of the material (textual records, maps, photographs, ...)', True, None, None),
    ('ISADG_DIMENSIONS', 'STRING', 'Dimensions', 'Dimensions', 'Dimensions of the item, e.g. "62 x 48 cm"', False, None, None),
    ('ISADG_SCALE', 'STRING', 'Scale', 'Scale', 'Scale of a map, plan or technical drawing, e.g. "1:2880"', False, None, None),
    ('ISADG_DURATION', 'INT', 'Duration (minutes)', 'Duration', 'Duration of a sound recording or moving images in minutes', False, None, None),
    # 3.2 Context
    ('ISADG_CREATOR', 'RECORD_REF', 'Creator', 'Creator', 'ISAD(G) 3.2.1: the person, family or corporate body that created or accumulated the unit', False, None, AGENTS),
    ('ISADG_BIOGHIST', 'TEXT', 'Administrative / biographical history', 'Biog. history', 'ISAD(G) 3.2.2: history of the creator, when it is not kept with the entity', False, None, None),
    ('ISADG_CUSTODHIST', 'TEXT', 'Archival history', 'Arch. history', 'ISAD(G) 3.2.3: history of the custody of the unit', False, None, None),
    ('ISADG_ACQINFO', 'TEXT', 'Immediate source of acquisition or transfer', 'Acquisition', 'ISAD(G) 3.2.4: source from which the unit was acquired, with date and method', False, None, None),
    # 3.3 Content and structure
    ('ISADG_SCOPE_CONTENT', 'TEXT', 'Scope and content', 'Scope', 'ISAD(G) 3.3.1: scope, content and form of the unit', False, None, None),
    ('ISADG_APPRAISAL', 'TEXT', 'Appraisal, destruction and scheduling', 'Appraisal', 'ISAD(G) 3.3.2: appraisal and destruction actions and their basis', False, None, None),
    ('ISADG_ACCRUALS', 'TEXT', 'Accruals', 'Accruals', 'ISAD(G) 3.3.3: expected additions to the unit', False, None, None),
    ('ISADG_ARRANGEMENT', 'TEXT', 'System of arrangement', 'Arrangement', 'ISAD(G) 3.3.4: internal structure, order and classification of the unit', False, None, None),
    # 3.4 Conditions of access and use
    ('ISADG_ACCESS_CONDITIONS', 'TEXT', 'Conditions governing access', 'Access', 'ISAD(G) 3.4.1: legal and other conditions restricting or affecting access', False, None, None),
    ('ISADG_ACCESS_STATUS', 'ENUM', 'Access status', 'Status', 'ISAD(G) 3.4.1: whether the unit is open, restricted or closed', True, None, None),
    ('ISADG_ACCESS_OPEN_DATE', 'DATE', 'Open from', 'Open from', 'ISAD(G) 3.4.1: date from which a restricted or closed unit becomes open', False, None, None),
    ('ISADG_REPRODUCTION_CONDITIONS', 'TEXT', 'Conditions governing reproduction', 'Reproduction', 'ISAD(G) 3.4.2: restrictions on reproduction and copyright', False, None, None),
    ('ISADG_LANGUAGE', 'ENUM', 'Language of material', 'Language', 'ISAD(G) 3.4.3: language of the material', True, None, None),
    ('ISADG_SCRIPT', 'STRING', 'Script of material', 'Script', 'ISAD(G) 3.4.3: script or notation system of the material', False, None, None),
    ('ISADG_PHYSTECH', 'TEXT', 'Physical characteristics and technical requirements', 'Physical', 'ISAD(G) 3.4.4: physical condition and equipment needed to use the unit', False, None, None),
    ('ISADG_FINDING_AIDS', 'TEXT', 'Finding aids', 'Finding aids', 'ISAD(G) 3.4.5: finding aids that exist for the unit', False, None, None),
    # 3.5 Allied materials
    ('ISADG_ORIGINALS_LOC', 'TEXT', 'Existence and location of originals', 'Originals', 'ISAD(G) 3.5.1: where the originals are when the unit consists of copies', False, None, None),
    ('ISADG_COPIES_LOC', 'TEXT', 'Existence and location of copies', 'Copies', 'ISAD(G) 3.5.2: copies of the unit and where they are', False, None, None),
    ('ISADG_RELATED_MATERIAL', 'TEXT', 'Related units of description', 'Related', 'ISAD(G) 3.5.3: related units in the same or another repository', False, None, None),
    ('ISADG_RELATED_LINK', 'URI_REF', 'Link to related material', 'Link', 'ISAD(G) 3.5.3: link to a related unit or resource', False, None, None),
    ('ISADG_BIBLIOGRAPHY', 'TEXT', 'Publication note', 'Publications', 'ISAD(G) 3.5.4: publications about or based on the unit', False, None, None),
    # 3.6 Notes
    ('ISADG_NOTE', 'TEXT', 'Note', 'Note', 'ISAD(G) 3.6.1: information that fits no other element', False, None, None),
    ('ISADG_INTERNAL_NOTE', 'TEXT', 'Internal note', 'Internal note', 'Note for the archivists, not published', False, None, None),
    # 3.7 Description control
    ('ISADG_PROCESS_INFO', 'TEXT', "Archivist's note", 'Archivist', 'ISAD(G) 3.7.1: how the description was prepared and by whom', False, None, None),
    ('ISADG_ARCHIVIST', 'RECORD_REF', 'Archivist', 'Archivist', 'ISAD(G) 3.7.1: the person who prepared the description', False, None, ['PERSON_INDIVIDUAL']),
    ('ISADG_RULES', 'TEXT', 'Rules or conventions', 'Rules', 'ISAD(G) 3.7.2: rules and conventions the description follows', False, None, None),
    ('ISADG_DESCRIPTION_DATE', 'TEXT', 'Date(s) of descriptions', 'Described', 'ISAD(G) 3.7.3: when the description was prepared or revised', False, None, None),
    # beyond ISAD(G): container, digital object, access points
    ('ISADG_CONTAINER', 'STRUCTURED', 'Container', 'Container', 'Storage unit holding the unit: box, folder, volume ... with its number', False, 'ISADG_CONTAINER', None),
    ('ISADG_CONTAINER_KIND', 'ENUM', 'Kind of container', 'Kind', 'Kind of the storage unit', True, None, None),
    ('ISADG_CONTAINER_PREFIX', 'STRING', 'Container prefix', 'Prefix', 'Text before the number of the container', False, None, None),
    ('ISADG_CONTAINER_NUMBER', 'INT', 'Container number', 'Number', 'Number of the container', False, None, None),
    ('ISADG_CONTAINER_POSTFIX', 'STRING', 'Container postfix', 'Postfix', 'Text after the number of the container', False, None, None),
    ('ISADG_CONTAINER_LOCATION', 'STRING', 'Location', 'Location', 'Where the container is kept (depot, row, shelf)', False, None, None),
    ('ISADG_DAO_LINK', 'URI_REF', 'Digital object', 'Digital object', 'Link to a digital copy or a born-digital object of the unit', False, None, None),
    ('ISADG_ACCESS_POINT', 'RECORD_REF', 'Access point', 'Access point', 'Entity the unit is indexed by; the specification is its role (subject, place, author, ...)', True, None, None),
    # the finding aid (items of the output)
    ('ISADG_FA_TITLE', 'STRING', 'Title of the finding aid', 'FA title', 'Title printed on the finding aid', False, None, None),
    ('ISADG_FA_KIND', 'ENUM', 'Kind of finding aid', 'FA kind', 'Inventory, catalogue, guide or list', True, None, None),
    ('ISADG_FA_AUTHOR', 'TEXT', 'Compiled by', 'Compiled by', 'Who compiled the finding aid', False, None, None),
    ('ISADG_FA_DATE', 'DATE', 'Date of the finding aid', 'FA date', 'When the finding aid was completed', False, None, None),
    ('ISADG_FA_LANGUAGE', 'ENUM', 'Language of the finding aid', 'FA language', 'Language the finding aid is written in', True, None, None),
    ('ISADG_FA_NOTE', 'TEXT', 'Note to the finding aid', 'FA note', 'Introduction or note printed in the finding aid', False, None, None),
    ('ISADG_ATTACHMENT', 'FILE_REF', 'Attachment', 'Attachment', 'File attached to the finding aid', False, None, None),
]
cs_items = {  # Czech name and shortcut
    'ISADG_LEVEL': ('Úroveň popisu', 'Úroveň'), 'ISADG_REFERENCE_CODE': ('Referenční označení', 'Ref. označení'),
    'ISADG_OTHER_ID': ('Jiný identifikátor', 'Jiný id.'), 'ISADG_TITLE': ('Název', 'Název'),
    'ISADG_DATE': ('Datace vzniku', 'Datace'), 'ISADG_DATE_BULK': ('Datace převažující části', 'Převažující datace'),
    'ISADG_DATE_TEXT': ('Datace textem', 'Datace textem'), 'ISADG_EXTENT': ('Rozsah a nosič', 'Rozsah'),
    'ISADG_EXTENT_QUANTITY': ('Množství', 'Množství'), 'ISADG_MATERIAL_TYPE': ('Druh materiálu', 'Druh'),
    'ISADG_DIMENSIONS': ('Rozměry', 'Rozměry'), 'ISADG_SCALE': ('Měřítko', 'Měřítko'),
    'ISADG_DURATION': ('Stopáž (minuty)', 'Stopáž'), 'ISADG_CREATOR': ('Původce', 'Původce'),
    'ISADG_BIOGHIST': ('Dějiny původce', 'Dějiny původce'), 'ISADG_CUSTODHIST': ('Dějiny jednotky popisu', 'Dějiny JP'),
    'ISADG_ACQINFO': ('Přímý zdroj akvizice', 'Zdroj akvizice'), 'ISADG_SCOPE_CONTENT': ('Obsah a tematický popis', 'Obsah'),
    'ISADG_APPRAISAL': ('Hodnocení a skartace', 'Skartace'), 'ISADG_ACCRUALS': ('Budoucí přírůstky', 'Přírůstky'),
    'ISADG_ARRANGEMENT': ('Způsob uspořádání', 'Uspořádání'), 'ISADG_ACCESS_CONDITIONS': ('Podmínky přístupu', 'Podmínky přístupu'),
    'ISADG_ACCESS_STATUS': ('Stav přístupnosti', 'Přístupnost'), 'ISADG_ACCESS_OPEN_DATE': ('Přístupné od', 'Přístupné od'),
    'ISADG_REPRODUCTION_CONDITIONS': ('Podmínky reprodukce', 'Reprodukce'), 'ISADG_LANGUAGE': ('Jazyk materiálu', 'Jazyk'),
    'ISADG_SCRIPT': ('Písmo materiálu', 'Písmo'), 'ISADG_PHYSTECH': ('Fyzický stav a technické požadavky', 'Fyzický stav'),
    'ISADG_FINDING_AIDS': ('Archivní pomůcky', 'Pomůcky'), 'ISADG_ORIGINALS_LOC': ('Existence a umístění originálů', 'Originály'),
    'ISADG_COPIES_LOC': ('Existence a umístění kopií', 'Kopie'), 'ISADG_RELATED_MATERIAL': ('Příbuzné jednotky popisu', 'Příbuzné JP'),
    'ISADG_RELATED_LINK': ('Odkaz na příbuzný materiál', 'Odkaz'), 'ISADG_BIBLIOGRAPHY': ('Edice a literatura', 'Literatura'),
    'ISADG_NOTE': ('Poznámka', 'Poznámka'), 'ISADG_INTERNAL_NOTE': ('Interní poznámka', 'Interní pozn.'),
    'ISADG_PROCESS_INFO': ('Informace o zpracování', 'Zpracování'), 'ISADG_ARCHIVIST': ('Zpracovatel', 'Zpracovatel'),
    'ISADG_RULES': ('Pravidla a konvence', 'Pravidla'), 'ISADG_DESCRIPTION_DATE': ('Datum popisu', 'Datum popisu'),
    'ISADG_CONTAINER': ('Ukládací jednotka', 'Ukl. jednotka'), 'ISADG_CONTAINER_KIND': ('Druh ukládací jednotky', 'Druh'),
    'ISADG_CONTAINER_PREFIX': ('Prefix', 'Prefix'), 'ISADG_CONTAINER_NUMBER': ('Číslo', 'Číslo'),
    'ISADG_CONTAINER_POSTFIX': ('Postfix', 'Postfix'), 'ISADG_CONTAINER_LOCATION': ('Umístění', 'Umístění'),
    'ISADG_DAO_LINK': ('Digitální objekt', 'Digitalizát'), 'ISADG_ACCESS_POINT': ('Přístupový bod', 'Přístupový bod'),
    'ISADG_FA_TITLE': ('Název pomůcky', 'Název pomůcky'), 'ISADG_FA_KIND': ('Druh pomůcky', 'Druh pomůcky'),
    'ISADG_FA_AUTHOR': ('Sestavil', 'Sestavil'), 'ISADG_FA_DATE': ('Datum pomůcky', 'Datum'),
    'ISADG_FA_LANGUAGE': ('Jazyk pomůcky', 'Jazyk pomůcky'), 'ISADG_FA_NOTE': ('Poznámka k pomůcce', 'Poznámka'),
    'ISADG_ATTACHMENT': ('Příloha', 'Příloha'),
}
x = ['<item-types>', '    <!-- generated by generator/gen_isadg.py; the creator and the access points refer to the classes of ISAAR_CPF -->']
for code, dt, name, short, desc, spec, struct, apt in items:
    attrs = 'code="%s" data-type="%s"' % (code, dt) + (' structure-type="%s"' % struct if struct else '')
    x += ['    <item-type %s>' % attrs, '        <name>%s</name>' % esc(name),
          '        <shortcut>%s</shortcut>' % esc(short), '        <description>%s</description>' % esc(desc),
          '        <use-specification>%s</use-specification>' % str(spec).lower()]
    if apt:
        x.append('        <item-aptypes>')
        x += ['            <item-aptype register-type="%s"/>' % a for a in apt]
        x.append('        </item-aptypes>')
    x.append('    </item-type>')
x.append('</item-types>')
write('rul_item_type.xml', '\n'.join(x) + '\n')
item_codes = [i[0] for i in items]
CONTAINER_ITEMS = ['ISADG_CONTAINER_KIND', 'ISADG_CONTAINER_PREFIX', 'ISADG_CONTAINER_NUMBER', 'ISADG_CONTAINER_POSTFIX',
                   'ISADG_CONTAINER_LOCATION']
FA_ITEMS = ['ISADG_FA_TITLE', 'ISADG_FA_KIND', 'ISADG_FA_AUTHOR', 'ISADG_FA_DATE', 'ISADG_FA_LANGUAGE', 'ISADG_FA_NOTE',
            'ISADG_ATTACHMENT']
# elements offered on a unit of description (everything but the items of the container and of the output)
LEVEL_ITEMS = [c for c in item_codes if c not in CONTAINER_ITEMS and c not in FA_ITEMS]
REPEATABLE = ['ISADG_OTHER_ID', 'ISADG_EXTENT_QUANTITY', 'ISADG_MATERIAL_TYPE', 'ISADG_CREATOR', 'ISADG_LANGUAGE',
              'ISADG_RELATED_LINK', 'ISADG_NOTE', 'ISADG_ARCHIVIST', 'ISADG_CONTAINER', 'ISADG_DAO_LINK', 'ISADG_ACCESS_POINT',
              'ISADG_FA_LANGUAGE', 'ISADG_ATTACHMENT']

# ---------------------------------------------------------------- specifications
# (code, English name, Czech name, item types, classes of RECORD_REF)
levels = [('FONDS', 'Fonds', 'Fond'), ('COLLECTION', 'Collection', 'Sbírka'), ('SUBFONDS', 'Subfonds', 'Dílčí fond'),
          ('SERIES', 'Series', 'Série'), ('SUBSERIES', 'Subseries', 'Podsérie'), ('FILE', 'File', 'Spis'),
          ('ITEM', 'Item', 'Jednotlivost')]
LEVEL = {v: 'ISADG_LEVEL_' + v for v, n, c in levels}
# allowed children by the level of the parent (D2): a collection is a root like a fonds; a level may
# nest in its own kind from the subfonds down (ISAD(G) 3.1.4: "first level sub-fonds, second level sub-fonds")
CHILDREN = {
    'FONDS': ['SUBFONDS', 'SERIES', 'FILE', 'ITEM'],
    'COLLECTION': ['SUBFONDS', 'SERIES', 'FILE', 'ITEM'],
    'SUBFONDS': ['SUBFONDS', 'SERIES', 'FILE', 'ITEM'],
    'SERIES': ['SERIES', 'SUBSERIES', 'FILE', 'ITEM'],
    'SUBSERIES': ['SUBSERIES', 'FILE', 'ITEM'],
    'FILE': ['FILE', 'ITEM'],
    'ITEM': ['ITEM'],
}
vocab = {
    'ISADG_LEVEL': [('ISADG_LEVEL_' + v, n, c) for v, n, c in levels],
    'ISADG_OTHER_ID': [('ISADG_OTHER_ID_FORMER', 'Former reference code', 'Dřívější referenční označení'),
                       ('ISADG_OTHER_ID_ACCESSION', 'Accession number', 'Přírůstkové číslo'),
                       ('ISADG_OTHER_ID_OTHER', 'Other identifier', 'Jiný identifikátor')],
    'ISADG_EXTENT_QUANTITY': [('ISADG_QUANTITY_LINEAR_METRES', 'linear metres', 'běžné metry'), ('ISADG_QUANTITY_BOXES', 'boxes', 'kartony'),
                              ('ISADG_QUANTITY_FOLDERS', 'folders', 'složky'), ('ISADG_QUANTITY_VOLUMES', 'volumes', 'svazky'),
                              ('ISADG_QUANTITY_ITEMS', 'items', 'kusy'), ('ISADG_QUANTITY_SHEETS', 'sheets', 'listy'),
                              ('ISADG_QUANTITY_PAGES', 'pages', 'strany'), ('ISADG_QUANTITY_MINUTES', 'minutes', 'minuty'),
                              ('ISADG_QUANTITY_BYTES', 'bytes', 'bajty')],
    'ISADG_MATERIAL_TYPE': [('ISADG_MATERIAL_TEXTUAL', 'Textual records', 'Textové dokumenty'),
                            ('ISADG_MATERIAL_REGISTERS', 'Registers and official books', 'Úřední knihy a rejstříky'),
                            ('ISADG_MATERIAL_MANUSCRIPTS', 'Manuscripts', 'Rukopisy'), ('ISADG_MATERIAL_PRINTED', 'Printed matter', 'Tisky'),
                            ('ISADG_MATERIAL_MAPS', 'Maps and plans', 'Mapy a plány'), ('ISADG_MATERIAL_DRAWINGS', 'Technical drawings', 'Technické výkresy'),
                            ('ISADG_MATERIAL_GRAPHIC', 'Graphic material', 'Grafika'), ('ISADG_MATERIAL_PHOTOGRAPHS', 'Photographs', 'Fotografie'),
                            ('ISADG_MATERIAL_SOUND', 'Sound recordings', 'Zvukové záznamy'), ('ISADG_MATERIAL_MOVING_IMAGES', 'Moving images', 'Filmové a videozáznamy'),
                            ('ISADG_MATERIAL_ELECTRONIC', 'Electronic records', 'Elektronické dokumenty'), ('ISADG_MATERIAL_SEALS', 'Seals', 'Pečetě'),
                            ('ISADG_MATERIAL_COINS', 'Coins and medals', 'Mince a medaile'), ('ISADG_MATERIAL_OBJECTS', 'Three-dimensional objects', 'Trojrozměrné předměty'),
                            ('ISADG_MATERIAL_OTHER', 'Other', 'Jiné')],
    'ISADG_ACCESS_STATUS': [('ISADG_ACCESS_OPEN', 'Open', 'Přístupné'), ('ISADG_ACCESS_RESTRICTED', 'Restricted', 'Omezeně přístupné'),
                            ('ISADG_ACCESS_CLOSED', 'Closed', 'Nepřístupné')],
    'ISADG_CONTAINER_KIND': [('ISADG_CONTAINER_BOX', 'Box', 'Karton'), ('ISADG_CONTAINER_FOLDER', 'Folder', 'Složka'),
                             ('ISADG_CONTAINER_VOLUME', 'Volume', 'Svazek'), ('ISADG_CONTAINER_ENVELOPE', 'Envelope', 'Obálka'),
                             ('ISADG_CONTAINER_TUBE', 'Tube', 'Tubus'), ('ISADG_CONTAINER_OTHER', 'Other', 'Jiné')],
    'ISADG_FA_KIND': [('ISADG_FA_INVENTORY', 'Inventory', 'Inventář'), ('ISADG_FA_CATALOGUE', 'Catalogue', 'Katalog'),
                      ('ISADG_FA_GUIDE', 'Guide', 'Průvodce'), ('ISADG_FA_LIST', 'List', 'Soupis')],
}
# short forms of the kinds of container, used in the value of the container ("box 12")
CONTAINER_SHORT = {'ISADG_CONTAINER_BOX': 'box', 'ISADG_CONTAINER_FOLDER': 'folder', 'ISADG_CONTAINER_VOLUME': 'vol.',
                   'ISADG_CONTAINER_ENVELOPE': 'env.', 'ISADG_CONTAINER_TUBE': 'tube', 'ISADG_CONTAINER_OTHER': ''}
roles = [('ISADG_AP_SUBJECT', 'Subject', 'Předmět', ['TERM']), ('ISADG_AP_PLACE', 'Place', 'Místo', ['GEO']),
         ('ISADG_AP_PERSON', 'Person', 'Osoba', ['PERSON_INDIVIDUAL']), ('ISADG_AP_FAMILY', 'Family', 'Rod', ['FAMILY']),
         ('ISADG_AP_CORPORATE_BODY', 'Corporate body', 'Korporace', ['PARTY_GROUP']),
         ('ISADG_AP_AUTHOR', 'Author', 'Autor', AGENTS), ('ISADG_AP_OTHER', 'Other role', 'Jiná role', ALL_CLASSES)]

specs = []  # (code, name, description, shortcut, [item types], classes, czech name)
for it, values in vocab.items():
    item_name = next(i[2] for i in items if i[0] == it)
    for code, n, cs in values:
        short = CONTAINER_SHORT.get(code, n) if it == 'ISADG_CONTAINER_KIND' else n
        specs.append((code, n, item_name + ': ' + n, short, [it], None, cs))
for code, n, cs, apt in roles:
    specs.append((code, n, 'Access point: ' + n.lower(), n, ['ISADG_ACCESS_POINT'], apt, cs))
# languages: the ISO 639-2 list of ISAAR_CPF (shared codes), assigned to the two language items
iso = []
for line in open(ISAAR_ISO, encoding='utf-8'):
    b, t, a2, en, fr = line.rstrip('\n').split('|')
    if b == 'qaa-qtz':
        continue
    iso.append(('LNG_' + b, en.split(';')[0], en))
iso.sort(key=lambda e: e[1].encode('utf-8').upper())
cs_langs = {}
for m in re.finditer(r'<item-spec code="(LNG_[^"]+)">\s*<name>([^<]*)</name>', open(CZ_BASE_SPECS, encoding='utf-8').read()):
    cs_langs[m.group(1)] = m.group(2)
for c, n, full in iso:
    specs.append((c, n, 'Language: ' + full, n[:50], ['ISADG_LANGUAGE', 'ISADG_FA_LANGUAGE'], None, cs_langs.get(c)))
x = ['<item-specs>', '    <!-- generated by generator/gen_isadg.py; LNG_* are the ISO 639-2 languages of ISAAR_CPF, shared by code -->']
for code, name, desc, short, its, apt, cs in specs:
    x += ['    <item-spec code="%s">' % code, '        <name>%s</name>' % esc(name),
          '        <description>%s</description>' % esc(desc), '        <shortcut>%s</shortcut>' % esc(short)]
    x += ['        <item-type-assign code="%s"/>' % it for it in its]
    if apt:
        x.append('        <item-aptypes>')
        x += ['            <item-aptype register-type="%s"/>' % a for a in apt]
        x.append('        </item-aptypes>')
    x.append('    </item-spec>')
x.append('</item-specs>')
write('rul_item_spec.xml', '\n'.join(x) + '\n')

# ---------------------------------------------------------------- institution types
# An institution (the holder of a fonds) needs a type; the types are declared by packages and
# ISAAR_CPF has none, so an installation without CZ_BASE gets them here.
write('par_institution_type.xml', '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<institution-types>
    <institution-type code="ISADG_ARCHIVE">
        <name>Archive</name>
    </institution-type>
    <institution-type code="ISADG_LIBRARY">
        <name>Library</name>
    </institution-type>
    <institution-type code="ISADG_MUSEUM">
        <name>Museum</name>
    </institution-type>
    <institution-type code="ISADG_OTHER_INSTITUTION">
        <name>Other institution</name>
    </institution-type>
</institution-types>
''')

# ---------------------------------------------------------------- structured type: the container
write('rul_structure_type.xml', '''<structure-types>
    <structure-type code="ISADG_CONTAINER">
        <name>Containers</name>
    </structure-type>
</structure-types>
''')
write('rul_structure_definition.xml', '''<structure-definitions>
    <structure-definition structure-type="ISADG_CONTAINER" filename="Container.drl">
        <def-type>ATTRIBUTE_TYPES</def-type>
        <priority>100</priority>
    </structure-definition>
    <structure-definition structure-type="ISADG_CONTAINER" filename="Container.groovy">
        <def-type>SERIALIZED_VALUE</def-type>
        <priority>100</priority>
    </structure-definition>
    <structure-definition structure-type="ISADG_CONTAINER" filename="Container-parse.groovy">
        <def-type>PARSE_VALUE</def-type>
        <priority>100</priority>
    </structure-definition>
</structure-definitions>
''')
write('rul_structure_extension.xml', '<structure-extensions>\n</structure-extensions>\n')
write('rul_structure_extension_definition.xml', '<structure-extension-definitions>\n</structure-extension-definitions>\n')
write('rules/Container.drl', '''package ISADG;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulItemSpecExt;

rule "The number of the container is required"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_CONTAINER_NUMBER")
then
    $itemType.setType(RulItemType.Type.REQUIRED);
end

rule "The kind of the container is recommended"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_CONTAINER_KIND")
then
    $itemType.setType(RulItemType.Type.RECOMMENDED);
end

rule "Prefix, postfix and location are possible"
no-loop
when
    $itemType : RulItemTypeExt(code in ("ISADG_CONTAINER_PREFIX", "ISADG_CONTAINER_POSTFIX", "ISADG_CONTAINER_LOCATION"))
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
end

rule "Kinds of container"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_CONTAINER_KIND")
    $itemSpec : RulItemSpecExt() from $itemType.rulItemSpecList
then
    $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
end
''')
write('scripts/Container.groovy', '''package scripts

import groovy.transform.Field
import cz.tacr.elza.domain.ArrData
import cz.tacr.elza.domain.ArrStructuredItem
import cz.tacr.elza.domain.RulItemSpec
import org.apache.commons.lang3.StringUtils

/*
 * Value of a container: the kind, the prefix, the number and the postfix - "box 12", "folder 3a";
 * the sort value pads the number; the complement is the location.
 */

@Field def result = RESULT
@Field List<ArrStructuredItem> items = ITEMS
@Field static int leadingZeros = 8

generate()
return

void generate() {
    StringBuilder value = new StringBuilder()
    StringBuilder sortValue = new StringBuilder()

    String kind = shortcut("ISADG_CONTAINER_KIND")
    if (StringUtils.isNotBlank(kind)) {
        value.append(kind).append(" ")
        sortValue.append(kind).append(" ")
    }
    appendValue(value, "ISADG_CONTAINER_PREFIX")
    appendValue(sortValue, "ISADG_CONTAINER_PREFIX")

    String number = text("ISADG_CONTAINER_NUMBER")
    if (StringUtils.isNotBlank(number)) {
        int zeros = Container.leadingZeros - number.length()
        if (zeros > 0) {
            sortValue.append(StringUtils.repeat("0", zeros))
        }
        value.append(number)
        sortValue.append(number)
    }
    appendValue(value, "ISADG_CONTAINER_POSTFIX")
    appendValue(sortValue, "ISADG_CONTAINER_POSTFIX")

    result.setValue(value.toString().trim())
    result.setSortValue(sortValue.toString().trim())
    String location = text("ISADG_CONTAINER_LOCATION").trim()
    result.setComplement(StringUtils.isEmpty(location) ? null : location)
}

String shortcut(String itemTypeCode) {
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equals(itemTypeCode)) {
            RulItemSpec spec = item.getItemSpec()
            return spec == null ? "" : spec.getShortcut()
        }
    }
    return ""
}

String text(String itemTypeCode) {
    StringBuilder sb = new StringBuilder()
    appendValue(sb, itemTypeCode)
    return sb.toString()
}

void appendValue(StringBuilder sb, String itemTypeCode) {
    for (ArrStructuredItem item : items) {
        if (item.getItemType().getCode().equals(itemTypeCode)) {
            ArrData data = item.getData()
            if (data != null) {
                sb.append(data.getFulltextValue())
            }
        }
    }
}
''')
write('scripts/Container-parse.groovy', '''package scripts

import groovy.transform.Field
import org.apache.commons.lang3.StringUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

/* A typed container value "A12b" becomes the prefix "A", the number 12 and the postfix "b". */

@Field def result = RESULT
@Field String value = VALUE

parse()
return

void parse() {
    Matcher matcher = Pattern.compile("(.*?)(\\\\d+)(.*)").matcher(value)
    if (matcher.find()) {
        String prefix = matcher.group(1).trim()
        if (StringUtils.isNotEmpty(prefix)) {
            result.addItem("ISADG_CONTAINER_PREFIX", prefix)
        }
        result.addItem("ISADG_CONTAINER_NUMBER", Integer.parseInt(matcher.group(2)))
        String postfix = matcher.group(3).trim()
        if (StringUtils.isNotEmpty(postfix)) {
            result.addItem("ISADG_CONTAINER_POSTFIX", postfix)
        }
    }
}
''')

# ---------------------------------------------------------------- rule set
write('rul_rule_set.xml', '''<rule-sets>
    <rule-set code="ISADG">
        <name>ISAD(G) archival description</name>
        <rule-type>ARRANGEMENT</rule-type>
        <rule-item-type-filter>ItemTypeFilter.drl</rule-item-type-filter>
    </rule-set>
</rule-sets>
''')
write(RS + 'rul_arrangement_rule.xml', '''<arrangement-rules>
    <arrangement-rule filename="Validation.drl">
        <rule-type>CONFORMITY_INFO</rule-type>
        <priority>100</priority>
    </arrangement-rule>
    <arrangement-rule filename="NewLevel.drl">
        <rule-type>NEW_LEVEL</rule-type>
        <priority>100</priority>
    </arrangement-rule>
    <arrangement-rule filename="ChangeImpact.drl">
        <rule-type>CONFORMITY_IMPACT</rule-type>
        <priority>100</priority>
    </arrangement-rule>
    <arrangement-rule filename="AvailableItems.drl">
        <rule-type>ATTRIBUTE_TYPES</rule-type>
        <priority>100</priority>
    </arrangement-rule>
</arrangement-rules>
''')
write(RS + 'rul_arrangement_extension.xml', '<arrangement-extensions>\n</arrangement-extensions>\n')
write(RS + 'rul_extension_rule.xml', '<extension-rules>\n</extension-rules>\n')
write(RS + 'rul_policy_type.xml', '''<policy-types>
    <policy-type code="ISADG_POL_BASIC">
        <name>Basic</name>
    </policy-type>
    <policy-type code="ISADG_POL_REFERENCE_CODE">
        <name>Reference codes</name>
    </policy-type>
    <policy-type code="ISADG_POL_CONTAINER">
        <name>Containers</name>
    </policy-type>
</policy-types>
''')
write(RS + 'rul_output_type.xml', '''<output-types>
    <output-type code="ISADG_FINDING_AID" filename="Output_FindingAid.drl">
        <name>Finding aid</name>
    </output-type>
</output-types>
''')
write(RS + 'rul_template.xml', '''<templates>
    <template code="ISADG_FINDING_AID_XML" output-type="ISADG_FINDING_AID" mime-type="application/xml" extension="xml">
        <name>XML (ELZA data exchange)</name>
        <engine>DE_XML</engine>
        <directory>FINDING_AID_XML</directory>
    </template>
</templates>
''')
write(RS + 'rul_package_actions.xml', '''<package-actions>
    <package-action filename="ISADG_FUND_VALIDATION.yaml">
        <action-recommendeds>
            <action-recommended output-type="ISADG_FINDING_AID" />
        </action-recommendeds>
    </package-action>
    <!-- No generator of reference codes: ISAD(G) 3.1.1 prescribes no form for the code, the
         archivist assigns it and the validation keeps it unique in the fonds. -->
    <!-- The persistent sort is recognised by its kind (!PersistentSort), so the file may have any
         name; a file name is unique across the installation, which rules out ZP2015's. -->
    <package-action filename="ISADG_PERSISTENT_SORT.yaml" />
</package-actions>
''')
write(RS + 'bulk_actions/ISADG_FUND_VALIDATION.yaml', '!FundValidation\nname: Validation of the description\n')
write(RS + 'bulk_actions/ISADG_PERSISTENT_SORT.yaml', '!PersistentSort\nname: Persistent sort\nfastAction: true\n')
write(RS + 'rules/ItemTypeFilter.drl', '''package ISADG;

import cz.tacr.elza.drools.model.ItemType;

rule "Item types of ISAD(G)"
when $it: ItemType(code in (%s))
then
    $it.setPossible();
end
''' % q(item_codes))

# ---------------------------------------------------------------- available items
HDR = '''package ISADG;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.DescItem;

'''
a = [HDR, '''// Order of evaluation: salience 100 sets the defaults (policy, repeatability, the level), the rules
// without salience offer the elements, salience -100 decides what is required or recommended.

rule "Default policy of the items"
salience 100
no-loop
when
    $itemType : RulItemTypeExt()
then
    $itemType.setPolicyTypeCode("ISADG_POL_BASIC");
end

rule "Default policy of the specifications"
salience 100
no-loop
when
    $itemType : RulItemTypeExt()
    $itemSpec : RulItemSpecExt() from $itemType.rulItemSpecList
then
    $itemSpec.setPolicyTypeCode("ISADG_POL_BASIC");
end

rule "An element is given once unless said otherwise"
salience 100
no-loop
when
    $itemType : RulItemTypeExt(code in (%s))
then
    $itemType.setRepeatable(false);
end

rule "Elements given several times"
salience 100
no-loop
when
    $itemType : RulItemTypeExt(code in (%s))
then
    $itemType.setRepeatable(true);
end

rule "ISAD(G) 3.1.4: the level of description is required"
salience 100
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_LEVEL")
then
    $itemType.setType(RulItemType.Type.REQUIRED);
end

rule "ISAD(G) 3.1.4: the root is a fonds or a collection"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_LEVEL")
    ActiveLevel( parent == null )
    $itemSpec : RulItemSpecExt(code in ("ISADG_LEVEL_FONDS", "ISADG_LEVEL_COLLECTION")) from $itemType.rulItemSpecList
then
    $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
end

''' % (q([c for c in LEVEL_ITEMS if c not in REPEATABLE]), q([c for c in REPEATABLE if c in LEVEL_ITEMS]))]
for parent, children in CHILDREN.items():
    a.append('''rule "ISAD(G) 3.1.4: below a %s"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_LEVEL")
    ActiveLevel( parent != null && $parentItems : parent.ownDescItems )
    DescItem( type == "ISADG_LEVEL" && specCode == "%s" ) from $parentItems
    $itemSpec : RulItemSpecExt(code in (%s)) from $itemType.rulItemSpecList
then
    $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
end

''' % (parent.lower(), LEVEL[parent], q([LEVEL[c] for c in children])))
GATED = ['ISADG_SCALE', 'ISADG_DURATION', 'ISADG_CONTAINER', 'ISADG_LEVEL']
a.append('''rule "The elements of ISAD(G) are possible at every level"
no-loop
when
    $itemType : RulItemTypeExt(code in (%s))
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
end

rule "Every specification of a vocabulary is possible"
no-loop
when
    $itemType : RulItemTypeExt(code in (%s))
    $itemSpec : RulItemSpecExt() from $itemType.rulItemSpecList
then
    $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
end

rule "Scale for maps, plans and technical drawings"
no-loop
when
    $activeLevel : ActiveLevel()
    DescItem( type == "ISADG_MATERIAL_TYPE" && specCode in ("ISADG_MATERIAL_MAPS", "ISADG_MATERIAL_DRAWINGS") ) from $activeLevel.ownDescItems
    $itemType : RulItemTypeExt(code == "ISADG_SCALE")
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
end

rule "Duration for sound recordings and moving images"
no-loop
when
    $activeLevel : ActiveLevel()
    DescItem( type == "ISADG_MATERIAL_TYPE" && specCode in ("ISADG_MATERIAL_SOUND", "ISADG_MATERIAL_MOVING_IMAGES") ) from $activeLevel.ownDescItems
    $itemType : RulItemTypeExt(code == "ISADG_DURATION")
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
end

rule "A container at every level, the root included"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_CONTAINER")
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
    $itemType.setPolicyTypeCode("ISADG_POL_CONTAINER");
end

rule "ISAD(G) I.12: the essential elements are required at the fonds"
salience -100
no-loop
when
    ActiveLevel( parent == null )
    $itemType : RulItemTypeExt(code in ("ISADG_REFERENCE_CODE", "ISADG_TITLE", "ISADG_DATE", "ISADG_EXTENT", "ISADG_CREATOR"))
then
    $itemType.setType(RulItemType.Type.REQUIRED);
end

rule "ISAD(G) 3.1.2: the title is required at every level"
salience -100
no-loop
when
    ActiveLevel( parent != null )
    $itemType : RulItemTypeExt(code == "ISADG_TITLE")
then
    $itemType.setType(RulItemType.Type.REQUIRED);
end

rule "ISAD(G) 3.1.1, 3.1.3: the reference code and the dates are recommended below the fonds"
salience -100
no-loop
when
    ActiveLevel( parent != null )
    $itemType : RulItemTypeExt(code in ("ISADG_REFERENCE_CODE", "ISADG_DATE"))
then
    $itemType.setType(RulItemType.Type.RECOMMENDED);
end

rule "The reference code has its own policy"
salience -100
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_REFERENCE_CODE")
then
    $itemType.setPolicyTypeCode("ISADG_POL_REFERENCE_CODE");
end
''' % (q([c for c in LEVEL_ITEMS if c not in GATED]),
       q(['ISADG_OTHER_ID', 'ISADG_EXTENT_QUANTITY', 'ISADG_MATERIAL_TYPE', 'ISADG_ACCESS_STATUS', 'ISADG_LANGUAGE', 'ISADG_ACCESS_POINT'])))
write(RS + 'rules/AvailableItems.drl', ''.join(a))

# ---------------------------------------------------------------- new level scenarios
n = ['''package ISADG;

import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevel;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.NewLevelApproach;

global cz.tacr.elza.drools.model.NewLevelApproaches results;

// The name of a scenario is its identifier (the DAO_LEVEL_IMPORT setting names one); the names
// are the English names of the levels.

// The only root scenario: a new fonds gets it without a choice, since fund creation applies a
// scenario only when there is exactly one. An artificial collection is made by changing the level
// of the root to Collection.
rule "Fonds"
when
    NewLevel( parent == null )
then
    NewLevelApproach approach = results.create("Fonds");
    approach.addDescItem("ISADG_LEVEL", "ISADG_LEVEL_FONDS");
end

''']
names = {v: n_ for v, n_, c in levels}
for parent, children in CHILDREN.items():
    for child in children:
        n.append('''rule "%s below a %s"
when
    NewLevel( parent != null && $items : parent.ownDescItems )
    DescItem( type == "ISADG_LEVEL" && specCode == "%s" ) from $items
then
    NewLevelApproach approach = results.create("%s");
    approach.addDescItem("ISADG_LEVEL", "%s");
end

''' % (names[child], parent.lower(), LEVEL[parent], names[child], LEVEL[child]))
write(RS + 'rules/NewLevel.drl', ''.join(n))

# ---------------------------------------------------------------- validation
write(RS + 'rules/Validation.drl', '''package ISADG;

import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.domain.vo.DataValidationResults;

global DataValidationResults dvResults;

// Messages are keyed (ISADG/<KEY>); translations/cs.xml carries the Czech texts.

function int rank(String level) {
    if ("ISADG_LEVEL_FONDS".equals(level)) return 1;
    if ("ISADG_LEVEL_COLLECTION".equals(level)) return 1;
    if ("ISADG_LEVEL_SUBFONDS".equals(level)) return 2;
    if ("ISADG_LEVEL_SERIES".equals(level)) return 3;
    if ("ISADG_LEVEL_SUBSERIES".equals(level)) return 4;
    if ("ISADG_LEVEL_FILE".equals(level)) return 5;
    if ("ISADG_LEVEL_ITEM".equals(level)) return 6;
    return 0;
}

// A level fits below the parent's level when it ranks lower; from the subfonds down a level may also
// nest in its own kind (ISAD(G) 3.1.4: "first level sub-fonds, second level sub-fonds").
function boolean fitsBelow(String child, String parent) {
    int c = rank(child);
    int p = rank(parent);
    if (c == 0 || p == 0) return true;
    if (c > p) return true;
    return c == p && c >= 2;
}

// Whether the level carries a container of its own.
function boolean hasContainer(Level level) {
    if (level == null || level.getDescItems() == null) return false;
    for (DescItem item : level.getDescItems()) {
        if ("ISADG_CONTAINER".equals(item.getType())) return true;
    }
    return false;
}

rule "ISAD(G) 3.1.4: the root is a fonds or a collection"
when
    $activeLevel : ActiveLevel( parent == null )
    not DescItem( type == "ISADG_LEVEL" && specCode in ("ISADG_LEVEL_FONDS", "ISADG_LEVEL_COLLECTION") ) from $activeLevel.ownDescItems
then
    dvResults.createMissing("ISADG_LEVEL", "ROOT_LEVEL", "The root must have the level Fonds or Collection (ISAD(G) 3.1.4).", "ISADG_POL_BASIC");
end

rule "ISAD(G) 3.1.4: the level fits below the level of the parent"
when
    $activeLevel : ActiveLevel( parent != null && $parent : parent )
    DescItem( type == "ISADG_LEVEL" && $child : specCode ) from $activeLevel.ownDescItems
    Level( $parentItems : ownDescItems ) from $parent
    DescItem( type == "ISADG_LEVEL" && $parentLevel : specCode ) from $parentItems
    eval( !fitsBelow($child, $parentLevel) )
then
    dvResults.createMissing("ISADG_LEVEL", "LEVEL_ORDER", "The level of description does not fit below the level of the parent unit (ISAD(G) 3.1.4).", "ISADG_POL_BASIC");
end

rule "ISAD(G) 3.1.3: the dates lie within the dates of the parent"
when
    $activeLevel : ActiveLevel( $ownItems : ownDescItems && parent != null && $parent : parent )
    $date : DescItem( type == "ISADG_DATE" ) from $ownItems
    Level( $parentItems : descItems ) from $parent
    DescItem( type == "ISADG_DATE" && (normalizedFrom > $date.normalizedFrom || normalizedTo < $date.normalizedTo) ) from $parentItems
then
    dvResults.createMissing("ISADG_DATE", "DATE_OUTSIDE_PARENT", "The dates of creation lie outside the dates of the parent unit (ISAD(G) 3.1.3).", "ISADG_POL_BASIC");
end

// The code is assigned by the archivist in the convention of the archive; core answers whether
// another unit of the fonds carries the same value (DescItem.duplicateInFund, UNITID items only).
rule "ISAD(G) 3.1.1: the reference code is unique in the fonds"
when
    $activeLevel : ActiveLevel()
    $code : DescItem( type == "ISADG_REFERENCE_CODE" && duplicateInFund ) from $activeLevel.ownDescItems
then
    dvResults.createError($code.getDescItemId(), "REFERENCE_CODE_DUPLICATE", "The reference code is not unique in the fonds (ISAD(G) 3.1.1).", "ISADG_POL_REFERENCE_CODE");
end

// ISAD(G) has no container element and many conventions keep the location outside the finding aid,
// so a container is demanded only where a neighbour of the unit has one.
rule "A file or item without a container next to one with a container"
when
    $activeLevel : ActiveLevel( !hasChildren )
    DescItem( type == "ISADG_LEVEL" && specCode in ("ISADG_LEVEL_FILE", "ISADG_LEVEL_ITEM") ) from $activeLevel.ownDescItems
    not ( $level : Level( $items : descItems ) and DescItem( type == "ISADG_CONTAINER" ) from $items )
    eval( hasContainer($activeLevel.getSiblingBefore()) || hasContainer($activeLevel.getSiblingAfter()) )
then
    dvResults.createMissing("ISADG_CONTAINER", "CONTAINER_MISSING", "The unit has no container, while a neighbouring unit has one.", "ISADG_POL_CONTAINER");
end
''')
messages_cs = [
    ('ROOT_LEVEL', 'Kořen musí mít úroveň Fond nebo Sbírka (ISAD(G) 3.1.4).'),
    ('LEVEL_ORDER', 'Úroveň popisu neodpovídá úrovni nadřazené jednotky popisu (ISAD(G) 3.1.4).'),
    ('DATE_OUTSIDE_PARENT', 'Datace vzniku leží mimo dataci nadřazené jednotky popisu (ISAD(G) 3.1.3).'),
    ('REFERENCE_CODE_DUPLICATE', 'Referenční označení není v archivním souboru jedinečné (ISAD(G) 3.1.1).'),
    ('CONTAINER_MISSING', 'Jednotka popisu nemá ukládací jednotku, sousední jednotka ji má.'),
]

# ---------------------------------------------------------------- impact of changes
write(RS + 'rules/ChangeImpact.drl', '''package ISADG;

import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.DescItemChange;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.vo.NodeTypeOperation;

global java.util.Set results;

rule "The unit is validated again after every operation"
when
then
    results.add(RelatedNodeDirection.NODE);
end

rule "A changed level or container affects the units below"
when
    DescItem( type in ("ISADG_LEVEL", "ISADG_CONTAINER") )
    NodeTypeOperation( this == NodeTypeOperation.SAVE_DESC_ITEM )
then
    results.add(RelatedNodeDirection.DESCENDANTS);
end

rule "Added or removed dates affect the units below"
when
    DescItem( type == "ISADG_DATE", change in (DescItemChange.CREATE, DescItemChange.DELETE) )
    NodeTypeOperation( this == NodeTypeOperation.SAVE_DESC_ITEM )
then
    results.add(RelatedNodeDirection.DESCENDANTS);
end

rule "A created unit affects its parent and siblings"
when
    NodeTypeOperation( this == NodeTypeOperation.CREATE_NODE )
then
    results.add(RelatedNodeDirection.PARENT);
    results.add(RelatedNodeDirection.SIBLINGS);
end

rule "A deleted unit affects its parent and siblings"
when
    NodeTypeOperation( this == NodeTypeOperation.DELETE_NODE )
then
    results.add(RelatedNodeDirection.PARENT);
    results.add(RelatedNodeDirection.SIBLINGS);
end

rule "A unit moved under the same parent affects its siblings"
when
    NodeTypeOperation( this in (NodeTypeOperation.DISCONNECT_NODE_LOCAL, NodeTypeOperation.CONNECT_NODE_LOCAL) )
then
    results.add(RelatedNodeDirection.SIBLINGS);
end

rule "A unit moved away affects its old parent and siblings"
when
    NodeTypeOperation( this == NodeTypeOperation.DISCONNECT_NODE )
then
    results.add(RelatedNodeDirection.PARENT);
    results.add(RelatedNodeDirection.SIBLINGS);
end

rule "A unit moved in affects its new parent, siblings and the units below"
when
    NodeTypeOperation( this == NodeTypeOperation.CONNECT_NODE )
then
    results.add(RelatedNodeDirection.PARENT);
    results.add(RelatedNodeDirection.SIBLINGS);
    results.add(RelatedNodeDirection.DESCENDANTS);
end
''')

# ---------------------------------------------------------------- output: the finding aid
write(RS + 'rules/Output_FindingAid.drl', '''package ISADG;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulItemSpecExt;

rule "Required items of the finding aid"
no-loop
when
    $itemType : RulItemTypeExt(code in ("ISADG_FA_TITLE", "ISADG_FA_DATE"))
then
    $itemType.setType(RulItemType.Type.REQUIRED);
    $itemType.setRepeatable(false);
end

// The kind of a finding aid is a national typology, not an element of the standard or of EAD.
rule "The kind of the finding aid is possible"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_FA_KIND")
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
    $itemType.setRepeatable(false);
end

rule "The language of the finding aid is required, several are possible"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_FA_LANGUAGE")
then
    $itemType.setType(RulItemType.Type.REQUIRED);
    $itemType.setRepeatable(true);
end

rule "Recommended items of the finding aid"
no-loop
when
    $itemType : RulItemTypeExt(code == "ISADG_FA_AUTHOR")
then
    $itemType.setType(RulItemType.Type.RECOMMENDED);
    $itemType.setRepeatable(false);
end

rule "Possible items of the finding aid"
no-loop
when
    $itemType : RulItemTypeExt(code in ("ISADG_FA_NOTE", "ISADG_ATTACHMENT"))
then
    $itemType.setType(RulItemType.Type.POSSIBLE);
    $itemType.setRepeatable(true);
end

rule "Every kind and language of the finding aid"
no-loop
when
    $itemType : RulItemTypeExt(code in ("ISADG_FA_KIND", "ISADG_FA_LANGUAGE"))
    $itemSpec : RulItemSpecExt() from $itemType.rulItemSpecList
then
    $itemSpec.setType(RulItemSpec.Type.POSSIBLE);
end
''')

# ---------------------------------------------------------------- UI settings of the rule set
groups = [
    ('01_IDENTITY', 'Identity statement', 'Identifikace',
     [('ISADG_LEVEL', 1), ('ISADG_REFERENCE_CODE', 1), ('ISADG_OTHER_ID', 2), ('ISADG_TITLE', 0), ('ISADG_DATE', 1),
      ('ISADG_DATE_BULK', 1), ('ISADG_DATE_TEXT', 2), ('ISADG_EXTENT', 0), ('ISADG_EXTENT_QUANTITY', 1),
      ('ISADG_MATERIAL_TYPE', 2), ('ISADG_DIMENSIONS', 1), ('ISADG_SCALE', 1), ('ISADG_DURATION', 1)]),
    ('02_CONTEXT', 'Context', 'Kontext',
     [('ISADG_CREATOR', 0), ('ISADG_BIOGHIST', 0), ('ISADG_CUSTODHIST', 0), ('ISADG_ACQINFO', 0)]),
    ('03_CONTENT', 'Content and structure', 'Obsah a struktura',
     [('ISADG_SCOPE_CONTENT', 0), ('ISADG_APPRAISAL', 0), ('ISADG_ACCRUALS', 0), ('ISADG_ARRANGEMENT', 0)]),
    ('04_ACCESS', 'Conditions of access and use', 'Podmínky přístupu a užití',
     [('ISADG_ACCESS_STATUS', 1), ('ISADG_ACCESS_OPEN_DATE', 1), ('ISADG_ACCESS_CONDITIONS', 0),
      ('ISADG_REPRODUCTION_CONDITIONS', 0), ('ISADG_LANGUAGE', 1), ('ISADG_SCRIPT', 1), ('ISADG_PHYSTECH', 0),
      ('ISADG_FINDING_AIDS', 0)]),
    ('05_ALLIED', 'Allied materials', 'Související materiál',
     [('ISADG_ORIGINALS_LOC', 0), ('ISADG_COPIES_LOC', 0), ('ISADG_RELATED_MATERIAL', 0), ('ISADG_RELATED_LINK', 0),
      ('ISADG_BIBLIOGRAPHY', 0)]),
    ('06_NOTES', 'Notes', 'Poznámky', [('ISADG_NOTE', 0), ('ISADG_INTERNAL_NOTE', 0)]),
    ('07_CONTROL', 'Description control', 'Kontrola popisu',
     [('ISADG_ARCHIVIST', 2), ('ISADG_PROCESS_INFO', 0), ('ISADG_RULES', 0), ('ISADG_DESCRIPTION_DATE', 1)]),
    ('08_CONTAINER', 'Container', 'Ukládací jednotka',
     [('ISADG_CONTAINER', 1), ('ISADG_CONTAINER_KIND', 1), ('ISADG_CONTAINER_PREFIX', 1), ('ISADG_CONTAINER_NUMBER', 1),
      ('ISADG_CONTAINER_POSTFIX', 1), ('ISADG_CONTAINER_LOCATION', 2)]),
    ('09_ACCESS_POINTS', 'Access points and digital objects', 'Přístupové body a digitální objekty',
     [('ISADG_ACCESS_POINT', 0), ('ISADG_DAO_LINK', 0)]),
    ('10_FINDING_AID', 'Finding aid', 'Archivní pomůcka',
     [('ISADG_FA_TITLE', 0), ('ISADG_FA_KIND', 1), ('ISADG_FA_AUTHOR', 0), ('ISADG_FA_DATE', 1), ('ISADG_FA_LANGUAGE', 1),
      ('ISADG_FA_NOTE', 0), ('ISADG_ATTACHMENT', 2)]),
]
placed = [c for g in groups for c, w in g[3]]
assert sorted(placed) == sorted(item_codes), set(item_codes) ^ set(placed)
grid = [('ISADG_REFERENCE_CODE', 100), ('ISADG_LEVEL', 100), ('ISADG_TITLE', 250), ('ISADG_DATE', 100),
        ('ISADG_MATERIAL_TYPE', 150), ('ISADG_CONTAINER', 100), ('ISADG_CREATOR', 180)]
icons = {'FONDS': 'fa-sitemap', 'COLLECTION': 'fa-archive', 'SUBFONDS': 'fa-sitemap', 'SERIES': 'ez-serie', 'SUBSERIES': 'ez-serie',
         'FILE': 'fa-folder-o', 'ITEM': 'fa-file-text-o'}
u = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
     '<settings xmlns:ns2="fund-views" xmlns:ns3="type-groups" xmlns:ns4="allow-structure-types"',
     '          xmlns:ns5="fund-issues" xmlns:ns6="grid-view">',
     '    <allow-structure-types settings-type="STRUCTURE_TYPES" entity-type="RULE">',
     '        <ns4:type code="ISADG_CONTAINER"/>',
     '    </allow-structure-types>',
     '    <fund-views settings-type="FUND_VIEW" entity-type="RULE">',
     '        <ns2:default>',
     '            <title>&lt;Not set&gt;</title>',
     '            <strict-mode>true</strict-mode>',
     '            <tree separator="; ">',
     '                <item type="ISADG_REFERENCE_CODE" />',
     '                <item type="ISADG_TITLE" />',
     '                <item type="ISADG_DATE" />',
     '            </tree>',
     '            <accordion-left separator="; ">',
     '                <item type="ISADG_REFERENCE_CODE" />',
     '                <item type="ISADG_TITLE" />',
     '                <item type="ISADG_DATE" />',
     '            </accordion-left>',
     '            <accordion-right>',
     '                <item type="ISADG_LEVEL" />',
     '                <item type="ISADG_CONTAINER" />',
     '            </accordion-right>',
     '            <hierarchy type-code="ISADG_LEVEL" default-separator="|">']
for v, n_, c in levels:
    u += ['                <level spec-code="%s">' % LEVEL[v], '                    <icon>%s</icon>' % icons[v], '                </level>']
u += ['            </hierarchy>', '        </ns2:default>', '    </fund-views>',
      '    <type-groups settings-type="TYPE_GROUPS" entity-type="RULE">', '        <ns3:default>']
for code, name, cs, types in groups:
    u += ['            <group code="%s">' % code, '                <name>%s</name>' % esc(name)]
    u += ['                <type code="%s" width="%d"/>' % (c, w) for c, w in types]
    u.append('            </group>')
u += ['        </ns3:default>', '    </type-groups>', '    <grid-view settings-type="GRID_VIEW" entity-type="RULE">']
for c, w in grid:
    u += ['        <ns6:item-type code="%s">' % c, '            <show-default>true</show-default>',
          '            <width>%d</width>' % w, '        </ns6:item-type>']
u += ['    </grid-view>',
      '    <fund-issues settings-type="FUND_ISSUES" entity-type="RULE">',
      '        <ns5:issue-states>',
      '            <ns5:issue-state code="RESOLVED" icon="fa-check"/>',
      '            <ns5:issue-state code="REJECTED" icon="fa-times"/>',
      '            <ns5:issue-state code="INVALID" icon="fa-minus"/>',
      '        </ns5:issue-states>',
      '        <ns5:issue-types>',
      '            <ns5:issue-type code="IMPORTANT" color="#CF2A28"/>',
      '            <ns5:issue-type code="RECOMMENDED" color="#FE9900"/>',
      '            <ns5:issue-type code="MINOR" color="#FFFF00"/>',
      '        </ns5:issue-types>',
      '    </fund-issues>',
      '</settings>']
write(RS + 'ui_setting.xml', '\n'.join(u) + '\n')

# ---------------------------------------------------------------- UI settings of the package
FAVOURITE_LANGUAGES = ['LNG_eng', 'LNG_fre', 'LNG_ger', 'LNG_spa', 'LNG_lat']
# No node-search-settings (pinned search filters): the setting is global, not per rule set, and the
# Czech package already ships one naming its own items - a second one is refused (plan 2b, core places).
p = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
     '<settings xmlns:ns7="structure-type-settings" xmlns:ns10="dao-import-level">']
for it in ['ISADG_LANGUAGE', 'ISADG_FA_LANGUAGE']:
    p.append('    <favorite-item-specs code="%s">' % it)
    p += ['        <favorite-item>%s</favorite-item>' % l for l in FAVOURITE_LANGUAGES]
    p.append('    </favorite-item-specs>')
p += ['    <structure-type-settings code="STRUCT_TYPE_ISADG_CONTAINER">',
      '      <ns7:property code="prefixSeparator"> </ns7:property>',
      '      <ns7:property code="postfixSeparator"> </ns7:property>',
      '    </structure-type-settings>',
      '    <dao-import-level-settings settings-type="DAO_LEVEL_IMPORT" entity-type="FUND">',
      '      <ns10:default>',
      '        <scenario>Series</scenario>',
      '        <desc-item-type>ISADG_TITLE</desc-item-type>',
      '      </ns10:default>',
      '    </dao-import-level-settings>',
      '</settings>']
write('ui_setting.xml', '\n'.join(p) + '\n')

# ---------------------------------------------------------------- Czech translation
t = ['<?xml version="1.0" encoding="UTF-8"?>', '<translations lang="cs">',
     '    <!-- generated by generator/gen_isadg.py -->']
for code, dt, name, short, desc, spec, struct, apt in items:
    cs_name, cs_short = cs_items[code]
    t.append('    <t type="ITEM_TYPE" code="%s" field="name">%s</t>' % (code, esc(cs_name)))
    t.append('    <t type="ITEM_TYPE" code="%s" field="shortcut">%s</t>' % (code, esc(cs_short)))
for code, name, desc, short, its, apt, cs in specs:
    if cs:
        t.append('    <t type="ITEM_SPEC" code="%s" field="name">%s</t>' % (code, esc(cs)))
        if code.startswith('ISADG_'):
            t.append('    <t type="ITEM_SPEC" code="%s" field="shortcut">%s</t>' % (code, esc(cs if code not in CONTAINER_SHORT else CONTAINER_SHORT[code] or cs)))
t.append('    <t type="STRUCTURED_TYPE" code="ISADG_CONTAINER" field="name">Ukládací jednotky</t>')
for code, cs in [('ISADG_POL_BASIC', 'Základní'), ('ISADG_POL_REFERENCE_CODE', 'Referenční označení'), ('ISADG_POL_CONTAINER', 'Ukládací jednotky')]:
    t.append('    <t type="POLICY_TYPE" code="%s" field="name">%s</t>' % (code, cs))
t.append('    <t type="OUTPUT_TYPE" code="ISADG_FINDING_AID" field="name">Archivní pomůcka</t>')
t.append('    <t type="TEMPLATE" code="ISADG_FINDING_AID_XML" field="name">XML (výměnný formát ELZA)</t>')
for code, name, cs, types in groups:
    t.append('    <t type="TYPE_GROUP" code="ISADG/%s" field="name">%s</t>' % (code, esc(cs)))
for key, cs in messages_cs:
    t.append('    <t type="MESSAGE" code="%s/%s" field="text">%s</t>' % (PACKAGE, key, esc(cs)))
t.append('</translations>')
write('translations/cs.xml', '\n'.join(t) + '\n')

print('ok', len(items), 'item types,', len(specs), 'specifications,', len(iso), 'languages')
