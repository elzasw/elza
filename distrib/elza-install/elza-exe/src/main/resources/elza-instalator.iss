#define NazevApliakceInstalator "ELZA"
#define KodAplikace  "ELZA"
#define IdAplikace "{44F9EF89-0373-4BFD-8D36-C465DAB6E0A2}"
#define VerzeAplikaceWinInfo "0.0.12.0"
#define NazevAplikace "Elektronická evidence archiválií"
#define NazevFirmy "Technologická agentura ÈR"
#define Copyright "Technologická agentura ÈR © 2017"
;#define TomcatVersion "8.5.15"
;#define NazevInstalator  "ELZA-0.12.0"
;#define VerzeAplikace "0.12.0-SNAPSHOT"

[Setup]
DisableProgramGroupPage=yes
OutputDir=.\
WizardImageFile=compiler:WizModernImage-IS.bmp
WizardSmallImageFile=compiler:WizModernSmallImage-IS.bmp
LicenseFile=.\classes\licence.txt
SourceDir=..\
ShowLanguageDialog=no
ShowTasksTreeLines=True
UsePreviousAppDir=yes
DisableDirPage=auto
AlwaysShowGroupOnReadyPage=True
AlwaysShowDirOnReadyPage=True
DisableStartupPrompt=true
UsePreviousLanguage=False
AllowUNCPath=False
AllowRootDirectory=True
DefaultDirName={code:InstalacniAdresar}
AppId={code:IdAplikace}
AppName={#NazevApliakceInstalator}
AppVersion={code:VerzeAplikace}
OutputBaseFilename={#NazevInstalator}
DefaultGroupName={#KodAplikace}
VersionInfoVersion={#VerzeAplikaceWinInfo}
VersionInfoTextVersion={#VerzeAplikace}
VersionInfoProductName={#KodAplikace}
VersionInfoProductVersion={#VerzeAplikaceWinInfo}
VersionInfoProductTextVersion={#VerzeAplikace}
VersionInfoDescription={#NazevAplikace}
AppPublisher={#NazevFirmy}
VersionInfoCompany={#NazevFirmy}
AppCopyright={#Copyright}
VersionInfoCopyright={#Copyright}
SetupIconFile=.\classes\Nastaveni\favicon.ico
AllowCancelDuringInstall=False

[Files]
Source: "compiler:\WizModernSmallImage.bmp"; Flags: dontcopy
Source: "apache-tomcat-{#TomcatVersion}\*"; DestDir: "{app}\apache-tomcat"; Flags: recursesubdirs
Source: "jre\*"; DestDir: "{app}\apache-tomcat\jre"; Flags: recursesubdirs
Source: "ROOT\*"; DestDir: "{app}\apache-tomcat\webapps\ROOT"; Flags: recursesubdirs
Source: "classes\TestDb\*"; DestDir: "{app}\TestDb"; Flags: recursesubdirs
Source: "package-cz-base.zip"; DestDir: "{app}\import"
Source: "package-rules-simple-dev.zip"; DestDir: "{app}\import"
Source: "package-zp2015.zip"; DestDir: "{app}\import"
Source: "all-institutions-import.xml"; DestDir: "{app}\import"
Source: "classes\Nastaveni\elza.url"; DestDir: "{app}"; Flags: onlyifdoesntexist
Source: "classes\Nastaveni\elza.yaml"; DestDir: "{app}\apache-tomcat\config"; Flags: onlyifdoesntexist
Source: "classes\Nastaveni\start.bat"; DestDir: "{app}"; Flags: onlyifdoesntexist
Source: "classes\Nastaveni\stop.bat"; DestDir: "{app}"; Flags: onlyifdoesntexist
Source: "classes\Nastaveni\favicon.ico"; DestDir: "{app}"
Source: "classes\Nastaveni\faviconuninstall.ico"; DestDir: "{app}"
Source: "classes\Nastaveni\faviconplay.ico"; DestDir: "{app}"
Source: "classes\Nastaveni\faviconstop.ico"; DestDir: "{app}"

[Icons]
Name: "{group}\{#KodAplikace} - Stránka aplikace"; Filename: "{app}\elza.url"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"
Name: "{group}\{#KodAplikace} - Odinstalovat aplikaci"; Filename: "{uninstallexe}"; IconFilename: "{app}\faviconuninstall.ico"
;Name: "{group}\{#KodAplikace} - Spustit službu"; Filename: "{app}\start.bat"; IconFilename: "{app}\faviconplay.ico"
;Name: "{group}\{#KodAplikace} - Vypnout službu"; Filename: "{app}\stop.bat"; IconFilename: "{app}\faviconstop.ico"

[Languages] 
Name: "Czech"; MessagesFile: "compiler:Languages\Czech.isl"

[LangOptions]
LanguageID=$0405

[PostCompile]

[Run]

[UninstallRun]
Filename: "net"; Parameters: "stop {#KodAplikace}"; Flags: runhidden
Filename: "{app}\apache-tomcat\bin\service.bat"; Parameters: "remove {#KodAplikace}"; Flags: runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{code:InstalacniAdresar}\apache-tomcat"

[Code]

//seznam konstant
const 
  //ID uživatelských obrazovek
  wpNastaveniDB = 100;

  //použité texty
  _Enter = #13#10;
  _Mezera = #32;
  _JdbcUrl = 'jdbc.url=';
  _JdbcUser = 'jdbc.username=';
  _JdbcPass = 'jdbc.password=';
  _Mssql = 'jdbc:jtds:sqlserver://';
  _Postgresql = 'jdbc:postgresql://';
  _TestDb = 'Otestovat pøipojení k databázi';
  _TestDbVysledek = 'Test pøipojení aplikaèní databáze: ';
  _TestJmeno = 'Uživatelské jméno: ';
  _TestOdpoved = 'Text odpovìdi: ';
  _TestOk = 'Pøipojení k databázi je v poøádku.';
  _PreskocitKontroluDB = 'Pøeskoèit kontrolu databáze';

  _NevyplnenoUrlServeru = 'Není vyplnìno povinné pole URL serveru!';
  _NevyplnenoPort = 'Není vyplnìno povinné pole port!'; 
  _NevyplnenoNazevDbApp = 'Není vyplnìno povinné pole název aplikaèní databáze!';
  _NevyplnenoNazevDbHist = 'Není vyplnìno povinné pole název historizaèní databáze!';
  _NevyplnenoJmeno = 'Není vyplnìno povinné pole jméno!';
  _NevyplnenoHeslo = 'Není vyplnìno povinné pole heslo!';
  _NevyplnenoSid = 'Není vyplnìno povinné pole SID!';
  _NevyplnenoSchemaApp = 'Není vyplnìno povinné pole název uživatelského schématu aplikaèní databáze!';
  _NevyplnenoSchemaHist = 'Není vyplnìno povinné pole název uživatelského schématu historizaèní databáze!';
  _NevyplnenoHesloApp = 'Není vyplnìno heslo k aplikaèní databázi!';
  _NevyplnenoHesloHist = 'Není vyplnìno heslo k historizaèní databázi!';

  _ChybaOdpoved = 'Nepodaøilo se získat odpovìï!';
  _ChybaTestu = 'Pøi testu pøipojení k databázi došlo k chybì: '; 
  
  _AktualizaceApp = 'Aktualizace aplikace:';
  _UlozeniNastaveniApp = 'Uložení nastavení aplikace';
  _UlozeniNastaveniPripojeni = 'Uložení nastavení pøipojení k databázi';
  _RegistaceSluzby = 'Registrace služby {#KodAplikace}';
  _OdstanovaniDocasnychSouboru = 'Odstraòování doèasných souborù';
  _Hotovo = 'Hotovo';
  _InstalacniAdresarObsahujeMezery = 'Ceska k instalaènímu adresáøi nesmí obsahovat mezery!';   

//globální promìnné
var 
  ComboTypDatabaze: TNewComboBox;

  LabelInstance, LabelNazevDB, LabelJmeno, LabelHeslo, StavDotazu, LabelOdinstalace, LabelUrlServeru, LabelPort, LabelAppPort : TNewStaticText;

  EditInstance, EditPort, EditUrlServeru, EditJmeno, EditHeslo, EditNazevDB, EditAppPort: TNewEdit;
  AdminJmeno, AdminHeslo, PortAplikace: String;

  ButtonTestPripojeniDB: TNewButton;

  ProgressBar : TNewProgressBar;

  CheckBox, CheckBoxZobrazStranku: TNewCheckBox;

  IsUpgrade : Boolean;
  
  SeznamUkolu: TNewCheckListBox;


//vrací id aplikace - pro sekci [setup]
function IdAplikace(Param: String): String;
begin
  result := '{#IdAplikace}';
end;

//vrací verzi aplikace - pro sekci [setup]
function VerzeAplikace(Param: String): String;
begin
  result := '{#VerzeAplikace}';
end;

//vrací výchozí adresáø instalace - pro sekci [setup]
function InstalacniAdresar(Param: String): String;
begin
  result := ExpandConstant('{sd}\{#KodAplikace}\');
end;

//funkce vrátí cestu k odinstalaènímu souboru podle jejího ID
function GetUninstallString(): String;
var
  sUnInstPath: String;
  sUnInstallString: String;
begin
  sUnInstPath := ExpandConstant('Software\Microsoft\Windows\CurrentVersion\Uninstall\{#emit SetupSetting("AppId")}_is1');
  sUnInstallString := '';
  if not RegQueryStringValue(HKLM, sUnInstPath, 'UninstallString', sUnInstallString) then
    RegQueryStringValue(HKCU, sUnInstPath, 'UninstallString', sUnInstallString);
  Result := sUnInstallString;
end;

//funkce vrátí verzi nainstalované aplikace podle jejího ID
function GetAppVersionString(): String;
var
  sUnInstPath: String;
  sUnInstallString: String;
begin
  sUnInstPath := ExpandConstant('Software\Microsoft\Windows\CurrentVersion\Uninstall\{#emit SetupSetting("AppId")}_is1');
  sUnInstallString := '';
  if not RegQueryStringValue(HKLM, sUnInstPath, 'DisplayVersion', sUnInstallString) then
    RegQueryStringValue(HKCU, sUnInstPath, 'DisplayVersion', sUnInstallString);
  Result := Trim (sUnInstallString);
end;

function NazevSluzby (Param: String):String;
begin
  Result := LowerCase ('{#KodAplikace}');
end;

//funkce nahradí hodnotu výchozí hodnotu hodnotou z instalátoru ve vybraném souboru
//parametry
//CestaKSouboru - cesta k souboru, kde se má provést náhrada hodnoty
//ZeStr - string, který se má nahradit
//DoStr - string, který má být uložen
function NahradHodnotuVSouboru(CestaKSouboru,ZeStr,DoStr:String):boolean;
var S: AnsiString;
    US: string;
begin
  result := false;
  
  //naète celý soubor do stringu
  if LoadStringFromFile(ExpandConstant(CestaKSouboru),S) then
  begin
    //nahradí výchozí hodnoty
    US := String(S);
    StringChangeEx(US, ZeStr , DoStr, True);
    //uloží zpìt do souboru
    if SaveStringToFile (ExpandConstant(CestaKSouboru), AnsiString(US), false) then
    begin
      result := true;
    end;
  end;
end;

//funkce spustí java aplikaci, která z pøedaných parametrù otestuje pøipojení k databázi a výsledek kontroly uloží do souboru testLog.txt, vrací text ovìøení
//vstupní parametry
//Parametr: parametr spuštìní java aplikace ve kterém se volá .jar pro spuštìní ovìøení a parametry pøipojení k databázi
//PracovniAdresar: pracovní adresáø do kterého se uloží výsledek ovìøení 
//TextChyba: text, který se vrací v pøípadì chyby rozšíøený o odpovìï serveru
function ProvedTestZalozeniDB(Parametr,PracovniAdresar,TextChyba: String): String;
var
  Odpoved : AnsiString;
  ResultCode: Integer;
begin
  if Exec(ExpandConstant('{app}\apache-tomcat\jre\bin\java.exe'), ExpandConstant(Parametr), ExpandConstant(PracovniAdresar), SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin

    if not LoadStringFromFile (ExpandConstant(PracovniAdresar) + 'testLog.txt',Odpoved) then
    begin
      result := TextChyba + _ChybaOdpoved + _enter;
      exit;
    end;

    if Pos ('ok',Odpoved ) = 1  then
    begin
      Odpoved := ''; //odpovìï je ok, vrací se prázdný string
    end
    else
    begin
      Odpoved := TextChyba + Odpoved + _Enter; //Chyba, vrací se text odpovìdi
    end;
    
    result := Odpoved;
  end
  else
  begin
    result := _ChybaTestu + SysErrorMessage(ResultCode) + _enter;
  end; 
end;

//funkce kontroluje povinná a duplicitní pole pro test pøipojení db - v pøípadì chyby vrací text s popisem - pokud je vše ok, vrací prázdný string
//vstupní parametry
//TypDB: 0 - MSSQL, 1 - Oracle
//UrlSeveru,Port,Sid,NazevDBApp,NazevDBHist,Jmeno,Heslo: Vstupní parametry k porovnání
function KontrolaPovinnaPole (TypDB:Integer; UrlSeveru,Port,Sid,NazevDB,Jmeno,Heslo: String):String;
var
  NevyplnenaPovinnaPole: String;
begin
  if (TypDB = 0) then
  begin
    if (UrlSeveru = '') then
      NevyplnenaPovinnaPole := _NevyplnenoUrlServeru + _Enter;
    if (Port = '') then
      NevyplnenaPovinnaPole := NevyplnenaPovinnaPole + _NevyplnenoPort + _Enter;
    if (NazevDB = '') then
      NevyplnenaPovinnaPole := NevyplnenaPovinnaPole + _NevyplnenoNazevDbApp + _Enter;
    if (Jmeno = '') then
      NevyplnenaPovinnaPole := NevyplnenaPovinnaPole + _NevyplnenoJmeno + _Enter;
    if (Heslo = '') then
      NevyplnenaPovinnaPole := NevyplnenaPovinnaPole + _NevyplnenoHeslo + _Enter;
  end;
  if (TypDB = 1) then
  begin
    
  end;
  
  result := NevyplnenaPovinnaPole;
end;

//funkce vrátí jdbc dle typu db a ze zadaných vstupních dat
//vstupní parametry
//TypDB: 0 - Postgresql, 1 - MSSQL
//UrlSeveru,Port,NazevDB,Sid: Vstupní parametry k vytvoøení jdbc
function VytvorJdbc(TypDB:Integer; UrlSeveru,Port,NazevDB,Instance: String):string;
var Jdbc: String;
begin
  if (TypDB = 0) then
  begin
    Jdbc := _Postgresql + UrlSeveru + ':' + Port + '/' + NazevDB;
  end
  else if (TypDB = 1) then
  begin
    Jdbc := _Mssql + UrlSeveru + ':' + Port + '/' + NazevDB;
    if Trim (Instance) <> '' then
    begin
      Jdbc := Jdbc + ';instance=' + Instance;
    end;
  end;
  
  result := Jdbc; 
end;

//funkce provede test pøipojení k databázi a vrátí stringový øetìzec
//pokud je test ok, vrací se prázdný øetìzec
//pokud dojde k chybì, vrací se popis chyby
function TestPripojeniDB():String;
var
  ResultCode: Integer;
  Jdbc, NeniVyplneno, OdpovedCela, Parametr, TextOdpoved, TestDbCesta, TestDbSoubor : String;
  
begin
  //cesta k adresáøi, kde se nachází jar pro testování db
  TestDbCesta := ExpandConstant ('{app}\TestDb\');
  TestDbSoubor := 'db.jar';
  
  //kontrola vyplnìní povinných polí
  NeniVyplneno := KontrolaPovinnaPole (ComboTypDatabaze.ItemIndex, EditUrlServeru.Text,EditPort.Text,EditInstance.Text,EditNazevDB.Text,EditJmeno.Text,EditHeslo.Text);
  if NeniVyplneno <> '' then
  begin
    result := NeniVyplneno;
    exit;
  end;
  
  //výbìr databáze Posgresql
  if ComboTypDatabaze.ItemIndex = 0 then 
  begin
    Jdbc := VytvorJdbc(ComboTypDatabaze.ItemIndex, EditUrlServeru.Text, EditPort.Text, EditNazevDB.Text, '');
    Parametr := ' -jar ' + ExpandConstant(TestDbCesta + TestDbSoubor) + ' "'+ Jdbc + '" "'+ EditJmeno.Text + '" "'+ EditHeslo.Text + '"'; 
    TextOdpoved := _TestDbVysledek + Jdbc + _Enter + _TestJmeno + EditJmeno.Text + _Enter + _TestOdpoved; 
    OdpovedCela := ProvedTestZalozeniDB(Parametr, TestDbCesta, TextOdpoved);
  end
  //výbìr databáze MSSQL
  else if ComboTypDatabaze.ItemIndex = 1 then 
  begin
    Jdbc := VytvorJdbc(ComboTypDatabaze.ItemIndex, EditUrlServeru.Text, EditPort.Text, EditNazevDB.Text, EditInstance.Text);
    Parametr := ' -jar ' + ExpandConstant(TestDbCesta + TestDbSoubor) + ' "'+ Jdbc + '" "'+ EditJmeno.Text + '" "'+ EditHeslo.Text + '"'; 
    TextOdpoved := _TestDbVysledek + Jdbc + _Enter + _TestJmeno + EditJmeno.Text + _Enter + _TestOdpoved; 
    OdpovedCela := ProvedTestZalozeniDB(Parametr, TestDbCesta, TextOdpoved);
  end
  //H2
  else if ComboTypDatabaze.ItemIndex = 0 then 
  begin
  end;
 
  result := OdpovedCela;
end;

//akce kontroly databáze spuštìná kliknutím na tlaèítko
procedure ButtonTestPripojeniDBOnClick(Sender: TObject);
var VysledekTestu : String;

begin
  //nastavení zobrazení prvkù
  CheckBOx.Checked := false;
  CheckBox.Visible := false;
  ProgressBar.Visible := true;

  //provedení testu DB
  VysledekTestu := TestPripojeniDB();
  //zobrazení zprávy s výsledkem
  if VysledekTestu = '' then
  begin
    MsgBox(_TestOk, mbInformation, mb_Ok);
  end
  else
  begin
    MsgBox(VysledekTestu, mbInformation, mb_Ok);
  end; 
  
  //skrytí ukazatele prùbìhu
  ProgressBar.Visible := false;
end;

//procedura ze vstupních parametrù poskládá string pro jdbc.properties a uloží k AS
//vstupní parametry
//TypDB: 0 - Postgresql, 1 - MSSQL, 2 - H2
procedure UlozJdbcDoSouboruProperties(TypDB:Integer; UrlSeveru,Port,Instance,NazevDB,Jmeno,Heslo: String);
var JdbcProperties: String;

begin
  if (ComboTypDatabaze.ItemIndex = 0) then
  begin
    
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbURL>', VytvorJdbc(TypDB, UrlSeveru, Port, NazevDB, ''));
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbUser>', Jmeno);
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbPass>', Heslo);
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbDialect>', 'dialect: org.hibernate.spatial.dialect.postgis.PostgisDialect');
  end
  else if ComboTypDatabaze.ItemIndex = 1 then
  begin
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbURL>', VytvorJdbc(TypDB, UrlSeveru, Port, NazevDB, ''));
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbUser>', Jmeno);
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbPass>', Heslo);
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbDialect>', 'dialect: org.hibernate.dialect.SQLServer2008Dialect');
  end
  else if ComboTypDatabaze.ItemIndex = 2 then
  begin
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbURL>', 'jdbc:h2:file:./elza.db;DB_CLOSE_DELAY=-1');
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbUser>', 'sa');
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbPass>', '');
    NahradHodnotuVSouboru('{app}\apache-tomcat\config\elza.yaml','<dbDialect>', 'dialect: org.hibernate.spatial.dialect.h2geodb.GeoDBDialect');
  end;end;

//spuštìné akce pøi zmìnì výbìru typu databáze v comboboxu
procedure ComboBoxOnChange(Sender: TObject);
begin
  //Výbìr databáze Postgresql - skryje Instanci 
  if ComboTypDatabaze.ItemIndex = 0 then 
  begin
    LabelUrlServeru.Visible := true;
    EditUrlServeru.Visible := true;
    LabelPort.Visible := true;
    EditPort.Visible := true;
    LabelInstance.Visible := false;
    EditInstance.Visible := false;
    LabelNazevDB.Visible := true;
    EditNazevDB.Visible := true;
    LabelJmeno.Visible := true;
    EditJmeno.Visible := true;
    LabelHeslo.Visible := true;
    EditHeslo.Visible := true;
    ButtonTestPripojeniDB.Visible := true;

    EditPort.Text := '5432';
    EditJmeno.PasswordChar := #0;
  end
  //Výbìr databáze MSSQL - zobrazí Instanci 
  else if ComboTypDatabaze.ItemIndex = 1 then 
  begin
    LabelUrlServeru.Visible := true;
    EditUrlServeru.Visible := true;
    LabelPort.Visible := true;
    EditPort.Visible := true;
    LabelInstance.Visible := true;
    EditInstance.Visible := true;
    LabelNazevDB.Visible := true;
    EditNazevDB.Visible := true;
    LabelJmeno.Visible := true;
    EditJmeno.Visible := true;
    LabelHeslo.Visible := true;
    EditHeslo.Visible := true;
    ButtonTestPripojeniDB.Visible := true;

    EditPort.Text := '1433';
  end
  //Výbìr embedded databáze 
  else if ComboTypDatabaze.ItemIndex = 2 then 
  begin 
    LabelUrlServeru.Visible := false;
    EditUrlServeru.Visible := false;
    LabelPort.Visible := false;
    EditPort.Visible := false;
    LabelInstance.Visible := false;
    EditInstance.Visible := false;
    LabelNazevDB.Visible := false;
    EditNazevDB.Visible := false;
    LabelJmeno.Visible := false;
    EditJmeno.Visible := false;
    LabelHeslo.Visible := false;
    EditHeslo.Visible := false;
    ButtonTestPripojeniDB.Visible := false;
  end;
end;
//funkce je volána pøed spuštìním instalátoru
function InitializeSetup(): Boolean;
var
  ResultCode: Integer;
begin
  result := true;
  
  //zjištìní, zda je již aplikace nainstalována
  //pokud ano, porovnává se verze aplikace a vrací se výsledek v promìnné IsUpgrade
  if (GetAppVersionString() <> '') and (Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')) <> '') then
  begin
    IsUpgrade := (Trim (GetAppVersionString()) <> Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')));
  end
  else
  begin
    IsUpgrade := false;
  end;
  
  //pokud pøedchozí kontrola vrátila, že se nejdená o aktualizaci a aplikace je již nainstalována
  if (IsUpgrade = false) and (Trim (GetUninstallString()) <> '') then
  begin 
    //je nainstalovaná shodná verze aplikace, pøepne se na upgrade
    IsUpgrade := true;
    result := true; 
  end;
end;

//procedura je volána pøi zmìnì obrazovky
procedure CurPageChanged(CurPageID: Integer);
var ResultCode, i:integer;
    Text, TextService: String;
begin
  
  i := 0;
  //pokud se jedná o aktualizaci
  if IsUpgrade then
  begin
    //doplnìní textu do pøípravy instalace - aktualizace
    if CurPageID=wpReady then
    begin
     Wizardform.ReadyMemo.Lines.Add('');
     Wizardform.ReadyMemo.Lines.Add(_AktualizaceApp);
     Wizardform.ReadyMemo.Lines.Add('      '+ GetAppVersionString()+' -> '+Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')));  
    end;
    //akce provedené pøed spuštìním instalace - aktualizace 
    //zastaví se služba a smaže adresáø lib  
    if CurPageID = 11 then
    begin
      WizardForm.BackButton.Enabled := False;
      Wizardform.NextButton.Enabled  := false;
      Wizardform.CancelButton.Enabled  := false;
      LabelOdinstalace := TNewStaticText.Create(WizardForm);
      LabelOdinstalace.AutoSize := True;
      LabelOdinstalace.WordWrap := true;
      LabelOdinstalace.Top := WizardForm.FinishedLabel.Top ;
      LabelOdinstalace.Left := ScaleX(40);
      LabelOdinstalace.Width := ScaleX(450);
      LabelOdinstalace.Caption := 'Provádí se zastavení služby aplikace..'+ #13#10 + #13#10 + 
                                  'Po zastavení služby dojde ke smazání souborù aplikace a nahrání nových. Na závìr se provede spuštìní služby.';
      LabelOdinstalace.Parent := WizardForm;
      WizardForm.Refresh;
      
      Exec('net', 'stop ' + NazevSluzby (''),'', SW_HIDE, ewWaitUntilTerminated, ResultCode);

      sleep (7000);

      FileCopy(ExpandConstant('{app}\apache-tomcat\conf\server.xml'),ExpandConstant('{tmp}\server.xml'), false)

      //smaže adresáøe tomcat
      DelTree(ExpandConstant('{app}\apache-tomcat\bin'), True, True, True);  
      DelTree(ExpandConstant('{app}\apache-tomcat\lib'), True, True, True);  
      DelTree(ExpandConstant('{app}\apache-tomcat\temp'), True, True, True);  

      //smaže adresáø s aplikací
      DelTree(ExpandConstant('{app}\apache-tomcat\webapps\'), True, True, True);
      LabelOdinstalace.Visible := false;
    end;
  end; 
  
  //akce provedené na stránce dokonèit
  if CurPageID = wpFinished then
  begin
    //zneplatní tlaèítko zpìt
    WizardForm.BackButton.Enabled := False;
    //doèasnì zneplatní tlaèítko dokonèit, do doby než se provedou všechny akce   
    Wizardform.NextButton.Enabled := false;
    
    WizardForm.FinishedLabel.Caption := '';
    

    PortAplikace := EditAppPort.Text;

    //vytvoøí objekt se seznamem úkolù
    SeznamUkolu := TNewCheckListBox.Create(WizardForm);
    SeznamUkolu.Top := WizardForm.FinishedLabel.Top ;
    SeznamUkolu.Left := WizardForm.FinishedLabel.Left;
    SeznamUkolu.Width := WizardForm.Width - SeznamUkolu.Left - ScaleX(20);
    SeznamUkolu.Height := ScaleY(205);
    SeznamUkolu.ShowLines := true;
    SeznamUkolu.Enabled := false;
    SeznamUkolu.Parent := WizardForm;


    //checkbox pro volbu zda se má zobrazit stránka aplikace
    CheckBoxZobrazStranku := TNewCheckBox.Create(WizardForm);
    CheckBoxZobrazStranku.Top := SeznamUkolu.Top + SeznamUkolu.Height + ScaleY(8);
    CheckBoxZobrazStranku.Left := WizardForm.FinishedLabel.Left;
    CheckBoxZobrazStranku.Width := WizardForm.Width;
    CheckBoxZobrazStranku.ParentBackground := true;
    CheckBoxZobrazStranku.ParentColor := true;
    CheckBoxZobrazStranku.Color := clWhite;
    CheckBoxZobrazStranku.Caption := 'Po dokonèení instalace zobrazit stránku aplikace.';
    CheckBoxZobrazStranku.Checked := True;
    CheckBoxZobrazStranku.Visible := False;
    CheckBoxZobrazStranku.Checked := false;
    CheckBoxZobrazStranku.Parent := WizardForm;

    //Pokud nejde o aktualizaci
    if IsUpgrade = false then
    begin
      //Nahradí pøeddefinované texty hodnotami z instalace
      SeznamUkolu.AddCheckBox(_UlozeniNastaveniApp, '', 0, false, false, false, false, nil);
      NahradHodnotuVSouboru('{app}\elza.url','<urlAplikace>',ExpandConstant('http://' + '{computername}' + ':' + PortAplikace)); //vytvoøí odkaz do start menu
      NahradHodnotuVSouboru('{app}\start.bat','<NazevSluzby>',NazevSluzby ('')); //nastaví název služby do dávky pro start služby
      NahradHodnotuVSouboru('{app}\stop.bat','<NazevSluzby>',NazevSluzby ('')); //nastaví název služby do dávky pro vypnutí služby
      
      TextService := 'set CATALINA_HOME=' + _Enter + 'set "JAVA_HOME='  + _Enter + '"set JRE_HOME=%cd%\..\jre"' + _Enter + 'setlocal'
      NahradHodnotuVSouboru('{app}\apache-tomcat\bin\service.bat','setlocal',TextService); //nastaví lokální JRE
      NahradHodnotuVSouboru('{app}\apache-tomcat\conf\server.xml','8080',PortAplikace); //nastaví port aplikace
                                     
      SeznamUkolu.Checked[i] := true;
      i := i + 1;

      //pokud není nastaveno pøeskoèit kontrolu, uloží se jdbc do souboru
      if CheckBOx.Checked = false then
      begin
        SeznamUkolu.AddCheckBox(_UlozeniNastaveniPripojeni, '', 0, false, false, false, false, nil);
        UlozJdbcDoSouboruProperties(ComboTypDatabaze.ItemIndex,EditUrlServeru.Text,EditPort.Text,EditInstance.Text,EditNazevDB.Text,EditJmeno.Text,EditHeslo.Text);
        SeznamUkolu.Checked[i] := true;
        i := i + 1;
      end;
      
      //zaregistruje se služba v systému
      SeznamUkolu.AddCheckBox(_RegistaceSluzby, '', 0, false, false, false, false, nil);
      Exec(ExpandConstant('{app}\apache-tomcat\bin\service.bat'), 'install ' + NazevSluzby (''), '',SW_HIDE, ewWaitUntilTerminated, ResultCode);
      SeznamUkolu.Checked[i] := true;
      i := i + 1;
      
    end; 

    //Pokud jde o aktualizaci, vrátí se zálohovaný server.xml (záloha se provede na zaèátku instalace)
    if IsUpgrade then
    begin
        FileCopy(ExpandConstant('{tmp}\server.xml'), ExpandConstant('{app}\apache-tomcat\conf\server.xml'), false);
    end;
    
    //smaže pomocné soubory
    SeznamUkolu.AddCheckBox(_OdstanovaniDocasnychSouboru, '', 0, false, false, false, false, nil);
    DelTree(ExpandConstant('{app}\TestDb\'), True, True, True);
    SeznamUkolu.Checked[i] := true;
    i := i + 1;
    
    if CheckBOx.Checked = false then
    begin
      SeznamUkolu.AddCheckBox('Start služby (operace mùže trvat delší dobu)', '', 0, false, false, false, false, nil);
      Exec('net', 'start ' + NazevSluzby (''), '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      if IsUpgrade = false then
      begin
        CheckBoxZobrazStranku.Visible := true;
        CheckBoxZobrazStranku.Checked := true;
      end;
      WizardForm.Refresh;
      sleep (10000);
      WizardForm.Refresh;
      sleep (10000);
      WizardForm.Refresh;
      sleep (10000);
      WizardForm.Refresh;
      sleep (10000);
      WizardForm.Refresh;
      sleep (10000);
      SeznamUkolu.Checked[i] := true;
      i := i + 1; 
      
      //Informace o pøístupu do aplikace 
      SeznamUkolu.AddCheckBox('Pro pøístup do aplikace je možné využít uživatelské jméno "admin" s heslem "admin".', '', 0, true, false, false, false, nil);
      SeznamUkolu.AddCheckBox('Pro správnou funkci aplikace je potøeba provést import balíèkù pravidel a osob. Zdrojové soubory se nachází v adresáøi import.', '', 0, true, false, false, false, nil);
    end
    else
    begin
      SeznamUkolu.AddCheckBox('Pro spuštìní aplikace je potøeba ruènì nastavit pøipojení k databázi v souboru elza.yaml a spustit službu.', '', 0, true, false, false, false, nil);
    end;

    //dokonèení instalace
    SeznamUkolu.AddCheckBox(_Hotovo, '', 0, true, false, false, false, nil);
    
    //zpøístupní se tlaèítko dokonèit
    Wizardform.NextButton.Enabled := true;
  end;
end;

//otevøe odkaz na nastavení aplikace ve výchozím prohlížeèi
procedure OdkazNastaveni(); 
var ErrorCode: Integer;
begin
  ShellExec('', ExpandConstant('http://' + '{computername}:' + PortAplikace), '', '', SW_SHOW, ewNoWait, ErrorCode);
end;

//funkce na pøeskonèení obrazovek
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  if IsUpgrade then
  begin
    case PageID of
      //seznam stránek, které budou pøeskoèeny pokud se jedná o aktualizaci
      wpLicense: Result := True;
      wpPreparing: Result := True;
      wpInstalling: Result := True;
      wpNastaveniDB: Result := true;
    else
      Result := False;
    end;
  end;
end;

//akce provedené po kliknutí na tlaèítko další
function NextButtonClick(CurPageID: Integer): Boolean;
var ResultCode:integer;
    VysledekTestu:String;
    
begin
  //kontrola cesty instalace - nesmí obsahovat mezery
  if CurPageID = wpSelectDir then
  begin
    if Pos (_Mezera,WizardDirValue ) > 0  then
    begin
      MsgBox(_InstalacniAdresarObsahujeMezery, mbError, mb_Ok);
    end
    else
    begin
      result := true;
    end;
  end
  //kliknutí na tlaèítko další na formuláøi s nastavením db
  else if CurPageID = wpNastaveniDB then
  begin
    //pokud je zaškrtnut checkbox, pøeskoèí sekontrola db a pokraèuje se dál
    if CheckBox.Checked then
    begin
      result := true;
    end
    //provede se kontrola db, pokud dojde k chybì, nepokraèuje se
    else
    begin
      CheckBox.Visible := false;
      ProgressBar.Visible := true;
      VysledekTestu := TestPripojeniDB();
      ProgressBar.Visible := false;
      if VysledekTestu = '' then
      begin
        result := true;
      end
      else
      begin
        MsgBox(VysledekTestu, mbInformation, mb_Ok);
        result := false;
      end; 
    end;
    
    if result then
    begin
      CheckBox.Visible := false
    end
    else
    begin
      CheckBox.Visible := true;
    end;
  end
  //kliknutí na talèítko dokonèit
  else if CurPageID = wpFinished then
  begin
    //otevøe odkaz na aplikaci ve výchozím prohlížeèi
    if CheckBoxZobrazStranku.Checked then 
    begin
      OdkazNastaveni();
    end;
    result := true;
  end
  else
  begin
    result := true;  
  end;  
end;

//definice uživatelských obrazovek
procedure CreateTheWizardPages;
var
  Page: TWizardPage;
  Panel: TPanel;
  PasswordEdit: TPasswordEdit;
  ListBox: TNewListBox;
  LabelTypDatabaze, ProgressBarLabel : TNewStaticText;
  FolderTreeView: TFolderTreeView;
  BitmapImage, BitmapImage2, BitmapImage3: TBitmapImage;
  BitmapFileName: String;
  RichEditViewer: TRichEditViewer;
  Row1Left, Row2Left : Integer;
begin
  Row1Left := 100;
  Row2Left := 205;

  Page := CreateCustomPage(wpInfoAfter, 'Konfigurace aplikace', 'Je nutné nastavení portu aplikace pod kterým bude k dispozici. Dále je nutné vyplnit údaje pro pøipojení k databázi.');
  
  LabelAppPort := TNewStaticText.Create(Page);
  LabelAppPort.Top := ScaleX(13);
  LabelAppPort.Caption := 'Port aplikace:';
  LabelAppPort.AutoSize := True;
  LabelAppPort.Parent := Page.Surface;
  
  EditAppPort := TNewEdit.Create(Page);
  EditAppPort.Top := ScaleX(10);
  EditAppPort.Left := ScaleX(Row1Left);
  EditAppPort.Width := Page.SurfaceWidth div 4 - ScaleX(8);
  EditAppPort.Text := '8080';
  EditAppPort.Parent := Page.Surface;

  LabelTypDatabaze := TNewStaticText.Create(Page);
  LabelTypDatabaze.Top := EditAppPort.Top + EditAppPort.Height + ScaleX(43);
  LabelTypDatabaze.Caption := 'Typ databáze:';
  LabelTypDatabaze.AutoSize := True;
  LabelTypDatabaze.Parent := Page.Surface;
  
  ComboTypDatabaze := TNewComboBox.Create(Page);
  ComboTypDatabaze.Left := ScaleX(Row1Left);
  ComboTypDatabaze.Top := EditAppPort.Top + EditAppPort.Height + ScaleX(40);
  ComboTypDatabaze.Width := Page.SurfaceWidth div 2 - ScaleX(26);
  ComboTypDatabaze.Parent := Page.Surface;
  ComboTypDatabaze.Style := csDropDownList;
  ComboTypDatabaze.Items.Add('PostgreSQL');
  ComboTypDatabaze.Items.Add('MSSQL');
  ComboTypDatabaze.Items.Add('Embedded databáze (H2)');
  ComboTypDatabaze.ItemIndex := 0;
  ComboTypDatabaze.OnChange := @ComboBoxOnChange;

  LabelUrlServeru := TNewStaticText.Create(Page);
  LabelUrlServeru.Top := ComboTypDatabaze.Top + ComboTypDatabaze.Height + ScaleX(23);
  LabelUrlServeru.Caption := 'Adresa serveru:';
  LabelUrlServeru.AutoSize := True;
  LabelUrlServeru.Parent := Page.Surface;
  
  EditUrlServeru := TNewEdit.Create(Page);
  EditUrlServeru.Top := ComboTypDatabaze.Top + ComboTypDatabaze.Height + ScaleX(20);
  EditUrlServeru.Left := ScaleX(Row1Left);
  EditUrlServeru.Width := Page.SurfaceWidth div 4 - ScaleX(8);
  EditUrlServeru.Parent := Page.Surface;
  
  LabelPort := TNewStaticText.Create(Page);
  LabelPort.Top := LabelUrlServeru.Top;
  LabelPort.Left := ScaleX(Row2Left);
  LabelPort.Caption := 'Port:';
  LabelPort.AutoSize := True;
  LabelPort.Parent := Page.Surface;
  
  EditPort := TNewEdit.Create(Page);
  EditPort.Top := EditUrlServeru.Top;
  EditPort.Left := LabelPort.Left + ScaleX(38);
  EditPort.Width := ScaleX(40);
  EditPort.Text := '5432';
  EditPort.Parent := Page.Surface;
  
  LabelInstance := TNewStaticText.Create(Page);
  LabelInstance.Top := LabelUrlServeru.Top;
  LabelInstance.Left := EditPort.Left + EditPort.Width + ScaleX(11);
  LabelInstance.Caption := 'Instance:';
  LabelInstance.AutoSize := True;
  LabelInstance.Visible := false;
  LabelInstance.Parent := Page.Surface;
  
  EditInstance := TNewEdit.Create(Page);
  EditInstance.Top := EditUrlServeru.Top;
  EditInstance.Left := LabelInstance.Left + LabelInstance.Width + ScaleX(14);
  EditInstance.Width := Page.SurfaceWidth div 6 - ScaleX(8);
  EditInstance.Visible := false;
  EditInstance.Parent := Page.Surface;
  
  LabelNazevDB := TNewStaticText.Create(Page);
  LabelNazevDB.Top := EditUrlServeru.Top + EditUrlServeru.Height + ScaleY(11);
  LabelNazevDB.Caption := 'Název databáze:';
  LabelNazevDB.AutoSize := True;
  LabelNazevDB.Parent := Page.Surface;
  
  EditNazevDB := TNewEdit.Create(Page);
  EditNazevDB.Top := EditUrlServeru.Top + EditUrlServeru.Height + ScaleY(8);
  EditNazevDB.Left := ScaleX(Row1Left);
  EditNazevDB.Width := Page.SurfaceWidth div 2 - ScaleX(26);
  EditNazevDB.Parent := Page.Surface;
  
  LabelJmeno := TNewStaticText.Create(Page);
  LabelJmeno.Top := EditNazevDB.Top + EditNazevDB.Height + ScaleY(11);
  LabelJmeno.Caption := 'Jméno:';
  LabelJmeno.AutoSize := True;
  LabelJmeno.Parent := Page.Surface;
  
  EditJmeno := TNewEdit.Create(Page);
  EditJmeno.Top := EditNazevDB.Top + EditNazevDB.Height + ScaleY(8);
  EditJmeno.Left := ScaleX(Row1Left);
  EditJmeno.Width := Page.SurfaceWidth div 4 - ScaleX(8);
  EditJmeno.Parent := Page.Surface;
  
  LabelHeslo := TNewStaticText.Create(Page);
  LabelHeslo.Top := EditNazevDB.Top + EditNazevDB.Height + ScaleY(11);
  LabelHeslo.Left := ScaleX(Row2Left);
  LabelHeslo.Caption := 'Heslo:';
  LabelHeslo.AutoSize := True;
  LabelHeslo.Parent := Page.Surface;
  
  EditHeslo := TNewEdit.Create(Page);
  EditHeslo.Top := EditNazevDB.Top + EditNazevDB.Height + ScaleY(8);
  EditHeslo.Left := LabelHeslo.Left + ScaleX(38);
  EditHeslo.Width := Page.SurfaceWidth div 4 - ScaleX(8);
  EditHeslo.PasswordChar := '*';
  EditHeslo.Parent := Page.Surface;
  
  ButtonTestPripojeniDB := TNewButton.Create(Page);
  ButtonTestPripojeniDB.Width := ScaleX(180);
  ButtonTestPripojeniDB.Height := ScaleY(23);
  ButtonTestPripojeniDB.Top := EditHeslo.Top + EditHeslo.Height + ScaleY(15);
  //ButtonTestPripojeniDB.Left :=  ButtonZalozeniDB.Left + ButtonZalozeniDB.Width + ScaleY(58); 
  ButtonTestPripojeniDB.Caption := 'Otestovat pøipojení k databázi';
  ButtonTestPripojeniDB.OnClick := @ButtonTestPripojeniDBOnClick;
  ButtonTestPripojeniDB.Parent := Page.Surface;
end;

//prvky v lištì s tlaèítky
procedure CreatePanelControl(ParentForm: TSetupForm; CancelButton: TNewButton);
begin
  ProgressBar := TNewProgressBar.Create(ParentForm);
  ProgressBar.Left := ParentForm.ClientWidth - CancelButton.Left - CancelButton.Width;
  ProgressBar.Top :=  CancelButton.Top;
  ProgressBar.Width := ScaleX(200);
  ProgressBar.Height := ScaleY(20);
  ProgressBar.Parent := ParentForm;
  ProgressBar.Style := npbstMarquee;
  ProgressBar.Visible := false;
  
  CheckBox := TNewCHeckBox.Create(ParentForm);
  CheckBox.Left := ParentForm.ClientWidth - CancelButton.Left - CancelButton.Width;
  CheckBox.Top := CancelButton.Top;
  CheckBox.Width := ScaleX(200);
  CheckBox.Caption := _PreskocitKontroluDB;
  CHeckBox.Visible := false;
  CheckBox.Parent := ParentForm;
  
  StavDotazu := TNewStaticText.Create(ParentForm);
  StavDotazu.Top := CancelButton.Top + ScaleX(4);
  StavDotazu.Left := ProgressBar.Left + ProgressBar.Width + ScaleX(10);
  StavDotazu.Caption := '';
  StavDotazu.AutoSize := True;
  StavDotazu.Parent := ParentForm;
  StavDotazu.Visible := false;
end;

procedure InitializeWizard();
begin
  CreateTheWizardPages;
  
  CreatePanelControl(WizardForm, WizardForm.CancelButton);
end;

procedure InitializeUninstallProgressForm();
begin
end;
