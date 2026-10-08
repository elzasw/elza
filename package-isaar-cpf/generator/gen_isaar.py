# Generates elza/package-isaar-cpf from data tables (step 2c.5b/c). Regenerate rather than hand-edit:
#   python generator/gen_isaar.py   (from any directory; writes the module this file lives in)
# iso639-2.txt is the ISO 639-2 code list of the Library of Congress
# (https://www.loc.gov/standards/iso639-2/ISO-639-2_utf-8.txt, BOM removed).
import os

GEN = os.path.dirname(os.path.abspath(__file__)).replace(os.sep, '/') + '/'
ROOT = os.path.dirname(GEN.rstrip('/')) + '/'
SRC = ROOT + 'src/'
RS = SRC + 'rul_rule_set/ISAAR_CPF/'


def write(path, text):
    full = (SRC if not path.startswith('/') else ROOT) + path.lstrip('/')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'w', encoding='utf-8', newline='').write(text)


def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


# ---------------------------------------------------------------- module files
write('/pom.xml', '''<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <artifactId>elza</artifactId>
        <groupId>cz.tacr.elza</groupId>
        <version>3.0-SNAPSHOT</version>
    </parent>
    <artifactId>package-isaar-cpf</artifactId>
    <name>Package ISAAR(CPF)</name>
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
                        <id>package-isaar-cpf</id>
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
  <id>base</id>
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
  <code>ISAAR_CPF</code>
  <name>ISAAR(CPF) entity description</name>
  <version>1</version>
  <description>International description of persons, families and corporate bodies according to ISAAR(CPF), with places and concepts; independent of the Czech CAM package (CZ_BASE), sharing its codes where the meaning is the same.</description>
  <language>en</language>
</package>
''')

# ---------------------------------------------------------------- classes
classes = [
    ('PERSON', None, 'Person or being', True),
    ('PERSON_INDIVIDUAL', 'PERSON', 'Person', False),
    ('DYNASTY', None, 'Family or dynasty', True),
    ('FAMILY', 'DYNASTY', 'Family', False),
    ('PARTY_GROUP', None, 'Corporate body', False),
    ('GEO', None, 'Place', False),
    ('TERM', None, 'Concept', False),
]
x = ['<ap-types>', '    <!-- shared with CZ_BASE (CAM) by code; PERSON and DYNASTY are declared as parents only -->']
for code, parent, name, ro in classes:
    attrs = 'code="%s"' % code + (' parent-ap-type="%s"' % parent if parent else '')
    x += ['    <ap-type %s>' % attrs, '        <name>%s</name>' % esc(name),
          '        <read-only>%s</read-only>' % str(ro).lower(), '    </ap-type>']
x.append('</ap-types>')
write('ap_type.xml', '\n'.join(x) + '\n')

# members and order of the rule set
members = ['PERSON_INDIVIDUAL', 'FAMILY', 'PARTY_GROUP', 'GEO', 'TERM']
write('rul_rule_set/ISAAR_CPF/rul_ap_type.xml', '<ap-types>\n' + ''.join('    <ap-type code="%s"/>\n' % c for c in members) + '</ap-types>\n')

# ---------------------------------------------------------------- part types
parts = [  # code, name, child, repeatable
    ('PT_NAME', 'Name', None, True),
    ('PT_IDENT', 'Identifier', None, True),
    ('PT_BODY', 'Description', None, False),
    ('PT_CRE', 'Beginning of existence', 'PT_REL', False),
    ('PT_EXT', 'End of existence', 'PT_REL', False),
    ('PT_EVENT', 'Event', 'PT_REL', True),
    ('PT_REL', 'Relation', None, True),
]
x = ['<part-types>', '    <!-- shared with CZ_BASE (CAM) by code, the same child parts and repeatability -->']
for code, name, child, rep in parts:
    x += ['    <part-type code="%s">' % code, '        <name>%s</name>' % name]
    if child:
        x.append('        <child_part>%s</child_part>' % child)
    x += ['        <repeatable>%s</repeatable>' % str(rep).lower(), '    </part-type>']
x.append('</part-types>')
write('rul_part_type.xml', '\n'.join(x) + '\n')
write('rul_rule_set/ISAAR_CPF/rul_part_type.xml', '<part-types>\n' + ''.join('    <part-type code="%s"/>\n' % p[0] for p in parts) + '</part-types>\n')

# ---------------------------------------------------------------- item types
# code, data type, name, shortcut, description, spec, limit, aptypes (owner's, for shared RECORD_REF)
items = [
    ('NM_MAIN', 'STRING', 'Name (main part)', 'Name', 'ISAAR(CPF) 5.1.2: main part of the name (surname of a person, name of a body or place)', False, 250, None),
    ('NM_MINOR', 'STRING', 'Name (other part)', 'Other part', 'Other part of the name (forenames of a person, qualifier of a body)', False, 250, None),
    ('NM_TYPE', 'ENUM', 'Form of name', 'Form', 'ISAAR(CPF) 5.1.3-5.1.5: kind of a non-authorized form of the name', True, None, None),
    ('NM_LANG', 'ENUM', 'Language of name', 'Language', 'Language of the form of the name', True, None, None),
    ('NM_USED_FROM', 'UNITDATE', 'Name used from', 'Used from', 'Beginning of the use of the form of the name', False, None, None),
    ('NM_USED_TO', 'UNITDATE', 'Name used to', 'Used to', 'End of the use of the form of the name', False, None, None),
    ('IDN_TYPE', 'ENUM', 'Identifier type', 'Id. type', 'ISAAR(CPF) 5.1.6, 5.4.1: source of the identifier', True, None, None),
    ('IDN_VALUE', 'STRING', 'Identifier', 'Identifier', 'Value of the identifier', False, 50, None),
    ('IDN_VALID_FROM', 'UNITDATE', 'Identifier valid from', 'Valid from', 'Beginning of the validity of the identifier', False, None, None),
    ('IDN_VALID_TO', 'UNITDATE', 'Identifier valid to', 'Valid to', 'End of the validity of the identifier', False, None, None),
    ('REL_ENTITY', 'RECORD_REF', 'Related entity', 'Related', 'ISAAR(CPF) 5.3: related entity; the specification is the category and kind of the relationship', True, None, None),
    ('REL_BEGIN', 'UNITDATE', 'Relationship from', 'From', 'ISAAR(CPF) 5.3.4: beginning of the relationship', False, None, None),
    ('REL_END', 'UNITDATE', 'Relationship to', 'To', 'ISAAR(CPF) 5.3.4: end of the relationship', False, None, None),
    ('CRE_DATE', 'UNITDATE', 'Date of birth / establishment', 'Beginning', 'ISAAR(CPF) 5.2.1: beginning of existence', False, None, None),
    ('CRE_CLASS', 'ENUM', 'Kind of beginning', 'Kind', 'What the beginning of existence is (birth, establishment, first mention)', True, None, None),
    ('EXT_DATE', 'UNITDATE', 'Date of death / dissolution', 'End', 'ISAAR(CPF) 5.2.1: end of existence', False, None, None),
    ('EXT_CLASS', 'ENUM', 'Kind of end', 'Kind', 'What the end of existence is (death, dissolution, last mention)', True, None, None),
    ('EV_TYPE', 'ENUM', 'Event type', 'Event', 'Kind of a dated event in the existence of the entity', True, None, None),
    ('EV_BEGIN', 'UNITDATE', 'Event from', 'From', 'Beginning of the event', False, None, None),
    ('EV_END', 'UNITDATE', 'Event to', 'To', 'End of the event', False, None, None),
    ('BRIEF_DESC', 'STRING', 'Abstract', 'Abstract', 'EAC-CPF abstract: one sentence about the entity', False, 250, None),
    ('HISTORY', 'TEXT', 'History', 'History', 'ISAAR(CPF) 5.2.2: history of the entity', False, None, None),
    ('GENEALOGY', 'TEXT', 'Genealogy', 'Genealogy', 'ISAAR(CPF) 5.2.7: genealogy of a family', False, None, None),
    ('CORP_STRUCTURE', 'TEXT', 'Internal structure', 'Structure', 'ISAAR(CPF) 5.2.7: internal structure of a corporate body', False, None, None),
    ('FOUNDING_NORMS', 'TEXT', 'Founding mandate', 'Founding mandate', 'ISAAR(CPF) 5.2.6: source of authority for the establishment', False, None, None),
    ('SCOPE_NORMS', 'TEXT', 'Mandates / sources of authority', 'Mandates', 'ISAAR(CPF) 5.2.6: mandates and sources of authority for functions and powers', False, None, None),
    ('SOURCE_INFO', 'TEXT', 'Sources', 'Sources', 'ISAAR(CPF) 5.4.8: sources consulted', False, None, None),
    ('SOURCE_LINK', 'URI_REF', 'Source link', 'Link', 'Link to a source', False, None, None),
    ('NOTE', 'TEXT', 'Note', 'Note', 'Note to the part', False, None, None),
    ('COORD_POINT', 'COORDINATES', 'Coordinates', 'Coordinates', 'Approximate location of the place (WGS 84)', False, None, None),
    ('GEO_ADMIN_CLASS', 'RECORD_REF', 'Part of (place)', 'Part of', 'The place this place belongs to', False, None, ['GEO']),
    # own
    ('ISAAR_CORP_TYPE', 'ENUM', 'Type of corporate body', 'Body type', 'ISAAR(CPF) 5.2.4, EAC-CPF otherEntityType, RiC CorporateBodyType: kind of the corporate body', True, None, None),
    ('ISAAR_LEGAL_STATUS', 'ENUM', 'Legal status', 'Legal status', 'ISAAR(CPF) 5.2.4, EAC-CPF legalStatus: legal form of the corporate body', True, None, None),
    ('ISAAR_FAMILY_TYPE', 'ENUM', 'Type of family', 'Family type', 'RiC FamilyType: kind of the family', True, None, None),
    ('ISAAR_PLACE_TYPE', 'ENUM', 'Type of place', 'Place type', 'EAC-CPF placeType (GeoNames feature classes): kind of the place', True, None, None),
    ('ISAAR_CONCEPT_SCHEME', 'ENUM', 'Concept scheme', 'Scheme', 'What the concept is used for (occupation, function, subject)', True, None, None),
    ('ISAAR_FUNCTIONS', 'TEXT', 'Functions, occupations and activities', 'Functions', 'ISAAR(CPF) 5.2.5: functions, occupations and activities as text; concepts are linked by relations', False, None, None),
    ('ISAAR_PLACES', 'TEXT', 'Places', 'Places', 'ISAAR(CPF) 5.2.3: places as text; places are linked by relations', False, None, None),
    ('ISAAR_GENERAL_CONTEXT', 'TEXT', 'General context', 'Context', 'ISAAR(CPF) 5.2.8: social, cultural, economic, political or historical context', False, None, None),
]
x = ['<item-types>', '    <!-- item types shared with CZ_BASE (CAM) by code, the same data types; ISAAR_* are own -->']
for code, dt, name, short, desc, spec, lim, apt in items:
    x += ['    <item-type code="%s" data-type="%s">' % (code, dt), '        <name>%s</name>' % esc(name),
          '        <shortcut>%s</shortcut>' % esc(short), '        <description>%s</description>' % esc(desc),
          '        <use-specification>%s</use-specification>' % str(spec).lower()]
    if lim:
        x.append('        <string-length-limit>%d</string-length-limit>' % lim)
    if apt:
        x.append('        <item-aptypes>')
        x += ['            <item-aptype register-type="%s"/>' % a for a in apt]
        x.append('        </item-aptypes>')
    x.append('    </item-type>')
x.append('</item-types>')
write('rul_item_type.xml', '\n'.join(x) + '\n')
item_codes = [i[0] for i in items]

# ---------------------------------------------------------------- specifications
# (code, name, description, assigned item type, aptypes)
specs = []
for c, n in [('NT_EQUIV', 'Parallel form'), ('NT_TRANSLATED', 'Translated form'), ('NT_OTHERRULES', 'Form by other rules'),
             ('NT_ALIAS', 'Alias / nickname'), ('NT_PSEUDONYM', 'Pseudonym'), ('NT_ACRONYM', 'Acronym / abbreviation'),
             ('NT_FORMER', 'Former form'), ('NT_NATIV', 'Birth name'), ('NT_HISTORICAL', 'Historical form'),
             ('NT_RELIGIOUS', 'Religious name')]:
    specs.append((c, n, 'Form of name: ' + n.lower(), 'NM_TYPE', None))
langs = [('LNG_eng', 'English'), ('LNG_cze', 'Czech'), ('LNG_ger', 'German'), ('LNG_fre', 'French'), ('LNG_spa', 'Spanish'),
         ('LNG_ita', 'Italian'), ('LNG_lat', 'Latin'), ('LNG_pol', 'Polish'), ('LNG_slo', 'Slovak'), ('LNG_rus', 'Russian'),
         ('LNG_hun', 'Hungarian'), ('LNG_dut', 'Dutch'), ('LNG_por', 'Portuguese'), ('LNG_swe', 'Swedish'),
         ('LNG_gre', 'Greek'), ('LNG_ukr', 'Ukrainian')]
for c, n in langs:
    specs.append((c, n, 'Language: ' + n, 'NM_LANG', None))
# then the rest of ISO 639-2 (bibliographic codes as in CAM, without qaa-qtz reserved for local use) by
# English name; the name is the first of the alternative names, the description lists them all
first = {c for c, n in langs}
iso = []
for line in open(GEN + 'iso639-2.txt', encoding='utf-8'):
    b, t, a2, en, fr = line.rstrip('\n').split('|')
    if b == 'qaa-qtz' or 'LNG_' + b in first:
        continue
    iso.append(('LNG_' + b, en.split(';')[0], en))
for c, n, full in sorted(iso, key=lambda e: e[1].encode('utf-8').upper()):
    specs.append((c, n, 'Language: ' + full, 'NM_LANG', None))
for c, n in [('VIAF', 'VIAF'), ('LCNAF', 'Library of Congress Name Authority File'), ('ORCID_ID', 'ORCID'),
             ('ISO3166_2', 'ISO 3166-1 alpha-2'), ('ISO3166_3', 'ISO 3166-1 alpha-3'),
             ('ISAAR_ISNI', 'ISNI'), ('ISAAR_WIKIDATA', 'Wikidata'), ('ISAAR_GND', 'GND'),
             ('ISAAR_GEONAMES', 'GeoNames'), ('ISAAR_TGN', 'Getty TGN')]:
    specs.append((c, n, 'Identifier type: ' + n, 'IDN_TYPE', None))
for c, n in [('CRC_BIRTH', 'Birth'), ('CRC_RISE', 'Establishment'), ('CRC_FIRSTWMENTION', 'First written mention')]:
    specs.append((c, n, 'Kind of beginning: ' + n.lower(), 'CRE_CLASS', None))
for c, n in [('EXC_DEATH', 'Death'), ('EXC_EXTINCTION', 'Dissolution'), ('EXC_LASTWMENTION', 'Last written mention')]:
    specs.append((c, n, 'Kind of end: ' + n.lower(), 'EXT_CLASS', None))
for c, n in [('ET_JOB', 'Occupation / office'), ('ET_MEMBERSHIP', 'Membership'), ('ET_STUDY', 'Study'), ('ET_AWARD', 'Award'),
             ('ISAAR_ET_LEGAL_STATUS', 'Change of legal status')]:
    specs.append((c, n, 'Event: ' + n.lower(), 'EV_TYPE', None))
P, PG, D, G, T = 'PERSON', 'PARTY_GROUP', 'DYNASTY', 'GEO', 'TERM'
# shared relation specifications with CAM's classes of related entities (must equal the owner's)
rels = [
    ('RT_FATHER', 'Father', 'family', [P]), ('RT_MOTHER', 'Mother', 'family', [P]),
    ('RT_HUSBAND', 'Husband', 'family', [P]), ('RT_WIFE', 'Wife', 'family', [P]),
    ('RT_PARTNER', 'Partner', 'family', [P]), ('RT_BROTHER', 'Brother', 'family', [P]),
    ('RT_SISTER', 'Sister', 'family', [P]), ('RT_RELATIONS', 'Other family relationship', 'family', [P]),
    ('RT_GENUSMEMBER', 'Member of family', 'family', [D]),
    ('RT_SUPCORP', 'Superior body', 'hierarchical', [PG]), ('RT_ISPART', 'Part of', 'hierarchical', [PG]),
    ('RT_ISMEMBER', 'Member of', 'hierarchical', [PG, D]),
    ('RT_EMPLOYER', 'Employer', 'hierarchical', [P, PG, D]), ('RT_FOUNDER', 'Founder', 'hierarchical', [P, PG, D]),
    ('RT_OWNER', 'Owner', 'hierarchical', [P, PG, D]),
    ('RT_PREDECESSOR', 'Predecessor', 'temporal', [P, PG, D]), ('RT_SUCCESSOR', 'Successor', 'temporal', [P, PG, D]),
    ('RT_OTHERNAME', 'Other identity of the same entity', 'identity', [P, PG]),
    ('RT_COLLABORATOR', 'Collaborator', 'associative', [P]),
    ('RT_RESIDENCE', 'Seat / residence', 'place', [G]), ('RT_PLACE', 'Place', 'place', [G]),
    ('RT_GEOSCOPE', 'Jurisdiction / geographic scope', 'place', [G]),
    ('RT_FUNCTION', 'Function / occupation', 'concept', [T]), ('RT_ACTIVITYFIELD', 'Field of activity', 'concept', [T]),
    ('RT_SUPTERM', 'Broader concept', 'concept', [T]), ('RT_RELATEDTERM', 'Related concept', 'concept', [T]),
    # own: CAM's RT_RELATED targets classes this package does not declare
    ('ISAAR_RT_ASSOCIATED', 'Associated entity', 'associative', [P, PG, D, G]),
]
for c, n, cat, apt in rels:
    specs.append((c, n, 'Relationship (%s): %s' % (cat, n.lower()), 'REL_ENTITY', apt))
own_vocab = {
    'ISAAR_CORP_TYPE': [('GOVERNMENT', 'Government or public administration body'), ('TERRITORIAL', 'Territorial self-government'),
                        ('JUDICIAL', 'Court or judicial body'), ('LEGISLATIVE', 'Legislative body'),
                        ('MILITARY', 'Military or security body'), ('PARTY', 'Political party'), ('RELIGIOUS', 'Religious body'),
                        ('EDUCATION', 'Educational or research institution'), ('HEALTH', 'Health or social-care institution'),
                        ('CULTURE', 'Cultural or heritage institution'), ('BUSINESS', 'Business enterprise'),
                        ('FINANCE', 'Financial institution'), ('PROFESSIONAL', 'Professional, trade or labour organisation'),
                        ('ASSOCIATION', 'Association or club'), ('FOUNDATION', 'Foundation or charitable body'),
                        ('INTERNATIONAL', 'International or umbrella organisation'), ('CONFERENCE', 'Conference or meeting'),
                        ('OTHER', 'Other / not determined')],
    'ISAAR_LEGAL_STATUS': [('PUBLIC_LAW', 'Public-law body'), ('PRIVATE_LAW', 'Private-law body'), ('COMPANY', 'Company with share capital'),
                           ('PARTNERSHIP', 'Partnership'), ('COOPERATIVE', 'Cooperative'), ('ASSOCIATION', 'Association'),
                           ('FOUNDATION', 'Foundation'), ('RELIGIOUS', 'Religious legal person'), ('SOLE_TRADER', 'Sole trader'),
                           ('NO_PERSONALITY', 'No legal personality'), ('OTHER', 'Other')],
    'ISAAR_FAMILY_TYPE': [('FAMILY', 'Family'), ('DYNASTY', 'Dynasty / royal house'), ('HOUSE', 'Noble house / lineage'),
                          ('CLAN', 'Clan / tribe'), ('BRANCH', 'Branch of a family')],
    'ISAAR_PLACE_TYPE': [('ADMINISTRATIVE', 'Administrative division'), ('AREA', 'Area'), ('WATER', 'Body of water'),
                         ('ELEVATION', 'Land elevation'), ('POPULATED', 'Populated place'), ('ROAD', 'Road or railroad'),
                         ('SPOT', 'Spot, building or farm'), ('UNDERSEA', 'Undersea'), ('VEGETATION', 'Vegetation')],
    'ISAAR_CONCEPT_SCHEME': [('OCCUPATION', 'Occupation'), ('FUNCTION', 'Function / activity'), ('SUBJECT', 'Subject'), ('OTHER', 'Other')],
}
own_spec_codes = {}
for it, values in own_vocab.items():
    own_spec_codes[it] = []
    for v, n in values:
        code = it.replace('ISAAR_', 'ISAAR_') + '_' + v
        own_spec_codes[it].append(code)
        item_name = next(i[2] for i in items if i[0] == it)
        specs.append((code, n, item_name + ': ' + n.lower(), it, None))
x = ['<item-specs>', '    <!-- specifications shared with CZ_BASE (CAM) by code (English texts, the same classes of related',
     '         entities); ISAAR_* are own -->']
for code, name, desc, it, apt in specs:
    x += ['    <item-spec code="%s">' % code, '        <name>%s</name>' % esc(name),
          '        <description>%s</description>' % esc(desc), '        <shortcut>%s</shortcut>' % esc(name if len(name) <= 50 else name.split(' (')[0][:50]),
          '        <item-type-assign code="%s"/>' % it]
    if apt:
        x.append('        <item-aptypes>')
        x += ['            <item-aptype register-type="%s"/>' % a for a in apt]
        x.append('        </item-aptypes>')
    x.append('    </item-spec>')
x.append('</item-specs>')
write('rul_item_spec.xml', '\n'.join(x) + '\n')

# ---------------------------------------------------------------- rule set
write('rul_rule_set.xml', '''<rule-sets>
    <rule-set code="ISAAR_CPF">
        <name>ISAAR(CPF) entity description</name>
        <rule-type>ENTITY</rule-type>
        <rule-item-type-filter>ItemTypeFilter.drl</rule-item-type-filter>
    </rule-set>
</rule-sets>
''')
write('rul_rule_set/ISAAR_CPF/rules/ItemTypeFilter.drl', '''package isaarcpf.itemtypes;

import cz.tacr.elza.drools.model.ItemType;

rule "ISAAR(CPF) item types"
when $it: ItemType(code in (%s))
then
    $it.setPossible();
end
''' % ', '.join('"%s"' % c for c in item_codes))

# ---------------------------------------------------------------- available items
HDR = '''package isaarcpf;
import cz.tacr.elza.drools.model.ItemType;
import cz.tacr.elza.drools.model.ItemSpec;
import cz.tacr.elza.drools.model.item.IntItem;
import cz.tacr.elza.drools.model.Part;
import cz.tacr.elza.drools.model.PartType;

'''


def rule_types(name, codes, action='setPossible', repeatable=False):
    body = '    $it.%s();\n' % action + ('    $it.setRepeatable(true);\n' if repeatable else '')
    return 'rule "%s"\nwhen $it: ItemType(code in (%s))\nthen\n%send\n\n' % (name, ', '.join('"%s"' % c for c in codes), body)


def rule_specs(name, item, spec_codes, item_action='setPossible', repeatable=False):
    body = '    $it.%s();\n    $is.setPossible();\n' % item_action + ('    $is.setRepeatable(true);\n' if repeatable else '')
    return ('rule "%s"\nwhen $it: ItemType(code == "%s")\n     $is: ItemSpec(code in (%s)) from $it.specs\nthen\n%send\n\n'
            % (name, item, ', '.join('"%s"' % c for c in spec_codes), body))


def rule_all_specs(name, item):
    return ('rule "%s"\nwhen $it: ItemType(code == "%s")\n     $is: ItemSpec() from $it.specs\nthen\n    $is.setPossible();\nend\n\n'
            % (name, item))


def drl(path, text):
    write('rul_rule_set/ISAAR_CPF/rules/' + path, HDR + text)


drl('available_items/GLOBAL.drl', rule_types('ISAAR: a note may be added to any part', ['NOTE'], repeatable=True))
drl('available_items/PT_NAME.drl',
    rule_types('ISAAR 5.1.2: the main part of the name is required', ['NM_MAIN'], 'setRequired')
    + rule_types('ISAAR 5.1.2: other parts and dates of use of the name', ['NM_MINOR', 'NM_USED_FROM', 'NM_USED_TO'])
    + rule_types('ISAAR 5.1.3: language of the name', ['NM_LANG'])
    + rule_all_specs('ISAAR 5.1.3: any language of the name', 'NM_LANG')
    + rule_specs('ISAAR 5.1.3-5.1.5: forms of the name', 'NM_TYPE',
                 ['NT_EQUIV', 'NT_TRANSLATED', 'NT_OTHERRULES', 'NT_ALIAS', 'NT_PSEUDONYM', 'NT_ACRONYM', 'NT_FORMER',
                  'NT_NATIV', 'NT_HISTORICAL', 'NT_RELIGIOUS']))
drl('available_items/PT_IDENT.drl',
    rule_types('ISAAR 5.1.6: type and value of the identifier are required', ['IDN_TYPE', 'IDN_VALUE'], 'setRequired')
    + rule_types('ISAAR 5.1.6: validity of the identifier', ['IDN_VALID_FROM', 'IDN_VALID_TO'])
    + rule_specs('ISAAR 5.4.1: authority identifiers', 'IDN_TYPE',
                 ['VIAF', 'LCNAF', 'ORCID_ID', 'ISAAR_ISNI', 'ISAAR_WIKIDATA', 'ISAAR_GND'], repeatable=True))
drl('available_items/GEO/PT_IDENT.drl',
    rule_specs('Place identifiers', 'IDN_TYPE', ['ISO3166_2', 'ISO3166_3', 'ISAAR_GEONAMES', 'ISAAR_TGN', 'ISAAR_WIKIDATA'],
               repeatable=True))
drl('available_items/PT_BODY.drl',
    rule_types('ISAAR 5.2: description of the entity', ['BRIEF_DESC', 'HISTORY', 'ISAAR_GENERAL_CONTEXT', 'SOURCE_INFO'])
    + rule_types('ISAAR 5.4.8: links to sources', ['SOURCE_LINK'], repeatable=True))
drl('available_items/PERSON_INDIVIDUAL/PT_BODY.drl',
    rule_types('ISAAR 5.2.3, 5.2.5: places and occupations of a person', ['ISAAR_PLACES', 'ISAAR_FUNCTIONS']))
drl('available_items/FAMILY/PT_BODY.drl',
    rule_types('ISAAR 5.2.3, 5.2.5, 5.2.7: places, activities and genealogy of a family', ['ISAAR_PLACES', 'ISAAR_FUNCTIONS', 'GENEALOGY'])
    + rule_specs('Type of family', 'ISAAR_FAMILY_TYPE', own_spec_codes['ISAAR_FAMILY_TYPE']))
drl('available_items/PARTY_GROUP/PT_BODY.drl',
    rule_types('ISAAR 5.2.3-5.2.7: places, functions, mandates and structure of a corporate body',
               ['ISAAR_PLACES', 'ISAAR_FUNCTIONS', 'FOUNDING_NORMS', 'SCOPE_NORMS', 'CORP_STRUCTURE'])
    + rule_specs('ISAAR 5.2.4: type of corporate body', 'ISAAR_CORP_TYPE', own_spec_codes['ISAAR_CORP_TYPE'], repeatable=True)
    + rule_specs('ISAAR 5.2.4: legal status', 'ISAAR_LEGAL_STATUS', own_spec_codes['ISAAR_LEGAL_STATUS']))
drl('available_items/GEO/PT_BODY.drl',
    rule_specs('Type of place is required', 'ISAAR_PLACE_TYPE', own_spec_codes['ISAAR_PLACE_TYPE'], 'setRequired')
    + rule_types('Location and hierarchy of the place', ['COORD_POINT', 'GEO_ADMIN_CLASS']))
drl('available_items/TERM/PT_BODY.drl',
    rule_specs('Scheme of the concept is required', 'ISAAR_CONCEPT_SCHEME', own_spec_codes['ISAAR_CONCEPT_SCHEME'], 'setRequired'))
drl('available_items/PT_CRE.drl', rule_types('ISAAR 5.2.1: date of the beginning of existence', ['CRE_DATE']))
drl('available_items/PT_EXT.drl', rule_types('ISAAR 5.2.1: date of the end of existence', ['EXT_DATE']))
for cls, cre, ext in [('PERSON_INDIVIDUAL', ['CRC_BIRTH'], ['EXC_DEATH']),
                      ('FAMILY', ['CRC_FIRSTWMENTION'], ['EXC_LASTWMENTION']),
                      ('PARTY_GROUP', ['CRC_RISE', 'CRC_FIRSTWMENTION'], ['EXC_EXTINCTION', 'EXC_LASTWMENTION']),
                      ('GEO', ['CRC_RISE', 'CRC_FIRSTWMENTION'], ['EXC_EXTINCTION', 'EXC_LASTWMENTION'])]:
    drl('available_items/%s/PT_CRE.drl' % cls, rule_specs('Kind of beginning', 'CRE_CLASS', cre))
    drl('available_items/%s/PT_EXT.drl' % cls, rule_specs('Kind of end', 'EXT_CLASS', ext))
drl('available_items/PT_EVENT.drl',
    rule_types('Dates of the event', ['EV_BEGIN', 'EV_END'])
    + rule_specs('Kind of event is required', 'EV_TYPE', ['ET_JOB', 'ET_MEMBERSHIP', 'ET_STUDY', 'ET_AWARD'], 'setRequired'))
drl('available_items/PARTY_GROUP/PT_EVENT.drl',
    rule_specs('ISAAR 5.2.4: dated change of legal status', 'EV_TYPE', ['ISAAR_ET_LEGAL_STATUS'], 'setRequired')
    + rule_specs('ISAAR 5.2.4: the new legal status', 'ISAAR_LEGAL_STATUS', own_spec_codes['ISAAR_LEGAL_STATUS']))
drl('available_items/PT_REL.drl',
    rule_types('ISAAR 5.3.1: the related entity is required', ['REL_ENTITY'], 'setRequired')
    + rule_types('ISAAR 5.3.4: dates of the relationship', ['REL_BEGIN', 'REL_END']))
rel_by_class = {
    'PERSON_INDIVIDUAL': ['RT_FATHER', 'RT_MOTHER', 'RT_HUSBAND', 'RT_WIFE', 'RT_PARTNER', 'RT_BROTHER', 'RT_SISTER', 'RT_RELATIONS',
                          'RT_GENUSMEMBER', 'RT_ISMEMBER', 'RT_EMPLOYER', 'RT_OTHERNAME', 'RT_COLLABORATOR', 'ISAAR_RT_ASSOCIATED',
                          'RT_RESIDENCE', 'RT_PLACE', 'RT_FUNCTION', 'RT_ACTIVITYFIELD'],
    'FAMILY': ['RT_ISMEMBER', 'RT_OWNER', 'RT_PREDECESSOR', 'RT_SUCCESSOR', 'ISAAR_RT_ASSOCIATED', 'RT_RESIDENCE', 'RT_PLACE',
               'RT_FUNCTION', 'RT_ACTIVITYFIELD'],
    'PARTY_GROUP': ['RT_SUPCORP', 'RT_ISPART', 'RT_ISMEMBER', 'RT_FOUNDER', 'RT_OWNER', 'RT_PREDECESSOR', 'RT_SUCCESSOR',
                    'RT_OTHERNAME', 'ISAAR_RT_ASSOCIATED', 'RT_RESIDENCE', 'RT_PLACE', 'RT_GEOSCOPE', 'RT_FUNCTION',
                    'RT_ACTIVITYFIELD'],
    'GEO': ['ISAAR_RT_ASSOCIATED'],
    'TERM': ['RT_SUPTERM', 'RT_RELATEDTERM', 'ISAAR_RT_ASSOCIATED'],
}
for cls, codes in rel_by_class.items():
    drl('available_items/%s/PT_REL.drl' % cls, rule_specs('ISAAR 5.3.2: relationships of the class', 'REL_ENTITY', codes))

# ---------------------------------------------------------------- validation
write('rul_rule_set/ISAAR_CPF/rules/validation/GLOBAL.drl', '''package isaarcpf;
import cz.tacr.elza.drools.model.ApValidationErrors;
import cz.tacr.elza.drools.model.Ap;
import cz.tacr.elza.drools.model.Part;
import cz.tacr.elza.drools.model.PartType;
global ApValidationErrors results;

rule "ISAAR 5.1.2: at least one name"
when $ae: Ap( $aeParts : parts )
     not (Part(type == PartType.PT_NAME) from $aeParts)
then
    results.addError("The entity must have at least one name; the preferred name is the authorized form.");
end

rule "ISAAR: empty part"
when $ae: Ap( $aeParts : parts )
     $part: Part(items.isEmpty()) from $aeParts
     not $childPart: Part(parentPartId == $part.id, type == PartType.PT_REL) from $ae.parts
then
    results.addError("The entity has an empty part.");
end

rule "ISAAR 5.2: one description"
when $ae: Ap( $aeParts : parts )
     $part: Part(type == PartType.PT_BODY) from $aeParts
     $part2: Part(type == PartType.PT_BODY && this != $part) from $aeParts
then
    results.addError("The description of the entity is given more than once.");
end

rule "ISAAR 5.2.1: one beginning of existence"
when $ae: Ap( $aeParts : parts )
     $part: Part(type == PartType.PT_CRE) from $aeParts
     $part2: Part(type == PartType.PT_CRE && this != $part) from $aeParts
then
    results.addError("The beginning of existence is given more than once.");
end

rule "ISAAR 5.2.1: one end of existence"
when $ae: Ap( $aeParts : parts )
     $part: Part(type == PartType.PT_EXT) from $aeParts
     $part2: Part(type == PartType.PT_EXT && this != $part) from $aeParts
then
    results.addError("The end of existence is given more than once.");
end
''')

# ---------------------------------------------------------------- index scripts
GH = '''package scripts

import cz.tacr.elza.groovy.GroovyAppender
import cz.tacr.elza.groovy.GroovyItem
import cz.tacr.elza.groovy.GroovyPart
import cz.tacr.elza.groovy.GroovyResult
import cz.tacr.elza.groovy.GroovyUtils

return generate(PART)

'''


def groovy(name, body):
    write('rul_rule_set/ISAAR_CPF/rules/index/' + name, GH + body)


groovy('PT_NAME.groovy', '''// name of a person, family, corporate body, place or concept: main part, other part
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("NM_MAIN")
    base.add("NM_MINOR").withSeparator(", ")

    GroovyResult result = new GroovyResult()
    String name = base.build()
    result.setDisplayName(name)
    result.setSortName(name)
    if (part.isPreferred()) {
        result.setPtPreferName(name)
    }
    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("NM_MAIN")
    result.addIndex("SHORT_NAME", shortName.build())
    return result
}
''')
groovy('PT_BODY.groovy', '''// description: the abstract, else the beginning of the history
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("BRIEF_DESC")
    String text = base.build()
    if (text == null || text.isEmpty()) {
        GroovyAppender history = GroovyUtils.createAppender(part)
        history.add("HISTORY")
        text = history.build()
        if (text != null && text.length() > 250) {
            text = text.substring(0, 247) + "..."
        }
    }
    if (text == null || text.isEmpty()) {
        text = "Description"
    }
    GroovyResult result = new GroovyResult()
    result.setDisplayName(text)
    return result
}
''')
groovy('PT_IDENT.groovy', '''// identifier: type and value; the pair is unique in the scope
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("IDN_TYPE")
    base.add("IDN_VALUE").withSeparator(": ")
    base.add("IDN_VALID_FROM").withSeparator(", ").withPrefix("valid from: ")
    base.add("IDN_VALID_TO").withSeparator(", ").withPrefix("valid to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addViewOrder("IDN_TYPE")
    sort.add("IDN_VALUE")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())

    GroovyAppender shortName = GroovyUtils.createAppender(part)
    shortName.add("IDN_TYPE")
    shortName.add("IDN_VALUE").withSeparator(": ")
    String key = shortName.build()
    result.addIndex("SHORT_NAME", key.toLowerCase())
    result.setKeyValue("PT_IDENT", key)
    return result
}
''')
for code, cls, dt in [('PT_CRE', 'CRE_CLASS', 'CRE_DATE'), ('PT_EXT', 'EXT_CLASS', 'EXT_DATE')]:
    groovy(code + '.groovy', '''// %s: kind, date and the related entities of the child parts
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender rels = GroovyUtils.createAppender(part)
    def children = part.getChildren()
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("%s")
    base.add("%s").withSeparator(", ")
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    String text = base.build()
    result.setDisplayName(text == null || text.isEmpty() ? "%s" : text)
    return result
}
''' % ('beginning of existence' if code == 'PT_CRE' else 'end of existence', cls, dt,
       'Beginning of existence' if code == 'PT_CRE' else 'End of existence'))
groovy('PT_EVENT.groovy', '''// event: kind, dates and the related entities of the child parts
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("EV_TYPE")
    base.add("ISAAR_LEGAL_STATUS").withSeparator(": ")
    base.add("EV_BEGIN").withSeparator(", ").withPrefix("from: ")
    base.add("EV_END").withSeparator(", ").withPrefix("to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addUnitdateFrom("EV_BEGIN")
    sort.addUnitdateTo("EV_END")
    sort.addViewOrder("EV_TYPE")

    GroovyAppender rels = GroovyUtils.createAppender(part)
    def children = part.getChildren()
    if (children != null) {
        for (GroovyPart childPart : children) {
            GroovyAppender childBase = GroovyUtils.createAppender(childPart)
            childBase.add("REL_ENTITY").withSpec()
            rels.addStr(childBase.build()).withSeparator(", ")
        }
    }
    base.addStr(rels.build()).withSeparator(" ").withPrefix("(").withPostfix(")")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())
    return result
}
''')
groovy('PT_REL.groovy', '''// relationship: the related entity with the kind of relationship and dates
static GroovyResult generate(final GroovyPart part) {
    GroovyAppender base = GroovyUtils.createAppender(part)
    base.add("REL_ENTITY").withSpec()
    base.add("REL_BEGIN").withSeparator(", ").withPrefix("from: ")
    base.add("REL_END").withSeparator(", ").withPrefix("to: ")

    GroovyAppender sort = GroovyUtils.createAppender(part)
    sort.addViewOrder("REL_ENTITY")
    sort.addUnitdateFrom("REL_BEGIN")
    sort.addUnitdateTo("REL_END")
    sort.add("REL_ENTITY")

    GroovyResult result = new GroovyResult()
    result.setDisplayName(base.build())
    result.setSortName(sort.build())
    return result
}
''')

# ---------------------------------------------------------------- entity rules file
rules = ['    <entity-rule filename="available_items/GLOBAL.drl" kind="AVAILABLE_ITEMS" priority="100"/>']
for pt in ['PT_NAME', 'PT_IDENT', 'PT_BODY', 'PT_CRE', 'PT_EXT', 'PT_EVENT', 'PT_REL']:
    rules.append('    <entity-rule filename="available_items/%s.drl" kind="AVAILABLE_ITEMS" part-type="%s" priority="100"/>' % (pt, pt))
per_class = [('GEO', 'PT_IDENT'), ('PERSON_INDIVIDUAL', 'PT_BODY'), ('FAMILY', 'PT_BODY'), ('PARTY_GROUP', 'PT_BODY'),
             ('GEO', 'PT_BODY'), ('TERM', 'PT_BODY'), ('PARTY_GROUP', 'PT_EVENT')]
for cls in ['PERSON_INDIVIDUAL', 'FAMILY', 'PARTY_GROUP', 'GEO']:
    per_class += [(cls, 'PT_CRE'), (cls, 'PT_EXT')]
for cls in rel_by_class:
    per_class.append((cls, 'PT_REL'))
for cls, pt in per_class:
    rules.append('    <entity-rule filename="available_items/%s/%s.drl" kind="AVAILABLE_ITEMS" ap-type="%s" part-type="%s" priority="100"/>'
                 % (cls, pt, cls, pt))
rules.append('    <entity-rule filename="validation/GLOBAL.drl" kind="VALIDATION" priority="100"/>')
for pt in ['PT_NAME', 'PT_BODY', 'PT_IDENT', 'PT_CRE', 'PT_EXT', 'PT_EVENT', 'PT_REL']:
    rules.append('    <entity-rule filename="index/%s.groovy" kind="INDEX" part-type="%s" priority="100"/>' % (pt, pt))
write('rul_rule_set/ISAAR_CPF/rul_entity_rule.xml', '<?xml version="1.0" encoding="UTF-8"?>\n<entity-rules>\n' + '\n'.join(rules) + '\n</entity-rules>\n')

# ---------------------------------------------------------------- UI settings: item positions per part
layout = {
    'PT_NAME': [('NM_MAIN', 6), ('NM_MINOR', 6), ('NM_TYPE', 4), ('NM_LANG', 4), ('NM_USED_FROM', 2), ('NM_USED_TO', 2), ('NOTE', 12)],
    'PT_IDENT': [('IDN_TYPE', 4), ('IDN_VALUE', 8), ('IDN_VALID_FROM', 3), ('IDN_VALID_TO', 3), ('NOTE', 12)],
    'PT_BODY': [('ISAAR_CORP_TYPE', 6), ('ISAAR_LEGAL_STATUS', 6), ('ISAAR_FAMILY_TYPE', 6), ('ISAAR_PLACE_TYPE', 6),
                ('ISAAR_CONCEPT_SCHEME', 6), ('COORD_POINT', 6), ('GEO_ADMIN_CLASS', 6), ('BRIEF_DESC', 12), ('HISTORY', 12),
                ('ISAAR_FUNCTIONS', 12), ('ISAAR_PLACES', 12), ('FOUNDING_NORMS', 12), ('SCOPE_NORMS', 12), ('CORP_STRUCTURE', 12),
                ('GENEALOGY', 12), ('ISAAR_GENERAL_CONTEXT', 12), ('SOURCE_INFO', 12), ('SOURCE_LINK', 12), ('NOTE', 12)],
    'PT_CRE': [('CRE_CLASS', 4), ('CRE_DATE', 4), ('NOTE', 12)],
    'PT_EXT': [('EXT_CLASS', 4), ('EXT_DATE', 4), ('NOTE', 12)],
    'PT_EVENT': [('EV_TYPE', 4), ('ISAAR_LEGAL_STATUS', 4), ('EV_BEGIN', 2), ('EV_END', 2), ('NOTE', 12)],
    'PT_REL': [('REL_ENTITY', 8), ('REL_BEGIN', 2), ('REL_END', 2), ('NOTE', 12)],
}
x = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
     '<settings xmlns:ns9="item-types">', '    <item-types entity-type="RULE">']
for pt, entries in layout.items():
    for pos, (code, width) in enumerate(entries, start=1):
        x += ['        <ns9:item-type code="%s">' % code, '            <position>%d</position>' % (pos * 10),
              '            <width>%d</width>' % width, '            <part-type>%s</part-type>' % pt, '        </ns9:item-type>']
x += ['    </item-types>', '</settings>']
write('rul_rule_set/ISAAR_CPF/ui_setting.xml', '\n'.join(x) + '\n')

print('ok', len(items), 'item types,', len(specs), 'specifications')
