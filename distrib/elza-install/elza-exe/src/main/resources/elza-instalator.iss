#define NazevApliakceInstalator "ELZA"
#define KodAplikace  "ELZA"
#define IdAplikace "{44F9EF89-0373-4BFD-8D36-C465DAB6E0A2}"
#define VerzeAplikaceWinInfo "0.0.12.0"
#define NazevAplikace "Elektronick� evidence archiv�li�"
#define NazevFirmy "Technologick� agentura �R"
#define Copyright "Technologick� agentura �R � 2017"
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
Name: "{group}\{#KodAplikace} - Str�nka aplikace"; Filename: "{app}\elza.url"; WorkingDir: "{app}"; IconFilename: "{app}\favicon.ico"
Name: "{group}\{#KodAplikace} - Odinstalovat aplikaci"; Filename: "{uninstallexe}"; IconFilename: "{app}\faviconuninstall.ico"
;Name: "{group}\{#KodAplikace} - Spustit slu�bu"; Filename: "{app}\start.bat"; IconFilename: "{app}\faviconplay.ico"
;Name: "{group}\{#KodAplikace} - Vypnout slu�bu"; Filename: "{app}\stop.bat"; IconFilename: "{app}\faviconstop.ico"

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
  //ID u�ivatelsk�ch obrazovek
  wpNastaveniDB = 100;

  //pou�it� texty
  _Enter = #13#10;
  _Mezera = #32;
  _JdbcUrl = 'jdbc.url=';
  _JdbcUser = 'jdbc.username=';
  _JdbcPass = 'jdbc.password=';
  _Mssql = 'jdbc:jtds:sqlserver://';
  _Postgresql = 'jdbc:postgresql://';
  _TestDb = 'Otestovat p�ipojen� k datab�zi';
  _TestDbVysledek = 'Test p�ipojen� aplika�n� datab�ze: ';
  _TestJmeno = 'U�ivatelsk� jm�no: ';
  _TestOdpoved = 'Text odpov�di: ';
  _TestOk = 'P�ipojen� k datab�zi je v po��dku.';
  _PreskocitKontroluDB = 'P�esko�it kontrolu datab�ze';

  _NevyplnenoUrlServeru = 'Nen� vypln�no povinn� pole URL serveru!';
  _NevyplnenoPort = 'Nen� vypln�no povinn� pole port!'; 
  _NevyplnenoNazevDbApp = 'Nen� vypln�no povinn� pole n�zev aplika�n� datab�ze!';
  _NevyplnenoNazevDbHist = 'Nen� vypln�no povinn� pole n�zev historiza�n� datab�ze!';
  _NevyplnenoJmeno = 'Nen� vypln�no povinn� pole jm�no!';
  _NevyplnenoHeslo = 'Nen� vypln�no povinn� pole heslo!';
  _NevyplnenoSid = 'Nen� vypln�no povinn� pole SID!';
  _NevyplnenoSchemaApp = 'Nen� vypln�no povinn� pole n�zev u�ivatelsk�ho sch�matu aplika�n� datab�ze!';
  _NevyplnenoSchemaHist = 'Nen� vypln�no povinn� pole n�zev u�ivatelsk�ho sch�matu historiza�n� datab�ze!';
  _NevyplnenoHesloApp = 'Nen� vypln�no heslo k aplika�n� datab�zi!';
  _NevyplnenoHesloHist = 'Nen� vypln�no heslo k historiza�n� datab�zi!';

  _ChybaOdpoved = 'Nepoda�ilo se z�skat odpov��!';
  _ChybaTestu = 'P�i testu p�ipojen� k datab�zi do�lo k chyb�: '; 
  
  _AktualizaceApp = 'Aktualizace aplikace:';
  _UlozeniNastaveniApp = 'Ulo�en� nastaven� aplikace';
  _UlozeniNastaveniPripojeni = 'Ulo�en� nastaven� p�ipojen� k datab�zi';
  _RegistaceSluzby = 'Registrace slu�by {#KodAplikace}';
  _OdstanovaniDocasnychSouboru = 'Odstra�ov�n� do�asn�ch soubor�';
  _Hotovo = 'Hotovo';
  _InstalacniAdresarObsahujeMezery = 'Ceska k instala�n�mu adres��i nesm� obsahovat mezery!';   

//glob�ln� prom�nn�
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


//vrac� id aplikace - pro sekci [setup]
function IdAplikace(Param: String): String;
begin
  result := '{#IdAplikace}';
end;

//vrac� verzi aplikace - pro sekci [setup]
function VerzeAplikace(Param: String): String;
begin
  result := '{#VerzeAplikace}';
end;

//vrac� v�choz� adres�� instalace - pro sekci [setup]
function InstalacniAdresar(Param: String): String;
begin
  result := ExpandConstant('{sd}\{#KodAplikace}\');
end;

//funkce vr�t� cestu k odinstala�n�mu souboru podle jej�ho ID
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

//funkce vr�t� verzi nainstalovan� aplikace podle jej�ho ID
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

//funkce nahrad� hodnotu v�choz� hodnotu hodnotou z instal�toru ve vybran�m souboru
//parametry
//CestaKSouboru - cesta k souboru, kde se m� prov�st n�hrada hodnoty
//ZeStr - string, kter� se m� nahradit
//DoStr - string, kter� m� b�t ulo�en
function NahradHodnotuVSouboru(CestaKSouboru,ZeStr,DoStr:String):boolean;
var S: AnsiString;
    US: string;
begin
  result := false;
  
  //na�te cel� soubor do stringu
  if LoadStringFromFile(ExpandConstant(CestaKSouboru),S) then
  begin
    //nahrad� v�choz� hodnoty
    US := String(S);
    StringChangeEx(US, ZeStr , DoStr, True);
    //ulo�� zp�t do souboru
    if SaveStringToFile (ExpandConstant(CestaKSouboru), AnsiString(US), false) then
    begin
      result := true;
    end;
  end;
end;

//funkce spust� java aplikaci, kter� z p�edan�ch parametr� otestuje p�ipojen� k datab�zi a v�sledek kontroly ulo�� do souboru testLog.txt, vrac� text ov��en�
//vstupn� parametry
//Parametr: parametr spu�t�n� java aplikace ve kter�m se vol� .jar pro spu�t�n� ov��en� a parametry p�ipojen� k datab�zi
//PracovniAdresar: pracovn� adres�� do kter�ho se ulo�� v�sledek ov��en� 
//TextChyba: text, kter� se vrac� v p��pad� chyby roz���en� o odpov�� serveru
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
      Odpoved := ''; //odpov�� je ok, vrac� se pr�zdn� string
    end
    else
    begin
      Odpoved := TextChyba + Odpoved + _Enter; //Chyba, vrac� se text odpov�di
    end;
    
    result := Odpoved;
  end
  else
  begin
    result := _ChybaTestu + SysErrorMessage(ResultCode) + _enter;
  end; 
end;

//funkce kontroluje povinn� a duplicitn� pole pro test p�ipojen� db - v p��pad� chyby vrac� text s popisem - pokud je v�e ok, vrac� pr�zdn� string
//vstupn� parametry
//TypDB: 0 - MSSQL, 1 - Oracle
//UrlSeveru,Port,Sid,NazevDBApp,NazevDBHist,Jmeno,Heslo: Vstupn� parametry k porovn�n�
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

//funkce vr�t� jdbc dle typu db a ze zadan�ch vstupn�ch dat
//vstupn� parametry
//TypDB: 0 - Postgresql, 1 - MSSQL
//UrlSeveru,Port,NazevDB,Sid: Vstupn� parametry k vytvo�en� jdbc
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

//funkce provede test p�ipojen� k datab�zi a vr�t� stringov� �et�zec
//pokud je test ok, vrac� se pr�zdn� �et�zec
//pokud dojde k chyb�, vrac� se popis chyby
function TestPripojeniDB():String;
var
  ResultCode: Integer;
  Jdbc, NeniVyplneno, OdpovedCela, Parametr, TextOdpoved, TestDbCesta, TestDbSoubor : String;
  
begin
  //cesta k adres��i, kde se nach�z� jar pro testov�n� db
  TestDbCesta := ExpandConstant ('{app}\TestDb\');
  TestDbSoubor := 'db.jar';
  
  //kontrola vypln�n� povinn�ch pol�
  NeniVyplneno := KontrolaPovinnaPole (ComboTypDatabaze.ItemIndex, EditUrlServeru.Text,EditPort.Text,EditInstance.Text,EditNazevDB.Text,EditJmeno.Text,EditHeslo.Text);
  if NeniVyplneno <> '' then
  begin
    result := NeniVyplneno;
    exit;
  end;
  
  //v�b�r datab�ze Posgresql
  if ComboTypDatabaze.ItemIndex = 0 then 
  begin
    Jdbc := VytvorJdbc(ComboTypDatabaze.ItemIndex, EditUrlServeru.Text, EditPort.Text, EditNazevDB.Text, '');
    Parametr := ' -jar ' + ExpandConstant(TestDbCesta + TestDbSoubor) + ' "'+ Jdbc + '" "'+ EditJmeno.Text + '" "'+ EditHeslo.Text + '"'; 
    TextOdpoved := _TestDbVysledek + Jdbc + _Enter + _TestJmeno + EditJmeno.Text + _Enter + _TestOdpoved; 
    OdpovedCela := ProvedTestZalozeniDB(Parametr, TestDbCesta, TextOdpoved);
  end
  //v�b�r datab�ze MSSQL
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

//akce kontroly datab�ze spu�t�n� kliknut�m na tla��tko
procedure ButtonTestPripojeniDBOnClick(Sender: TObject);
var VysledekTestu : String;

begin
  //nastaven� zobrazen� prvk�
  CheckBOx.Checked := false;
  CheckBox.Visible := false;
  ProgressBar.Visible := true;

  //proveden� testu DB
  VysledekTestu := TestPripojeniDB();
  //zobrazen� zpr�vy s v�sledkem
  if VysledekTestu = '' then
  begin
    MsgBox(_TestOk, mbInformation, mb_Ok);
  end
  else
  begin
    MsgBox(VysledekTestu, mbInformation, mb_Ok);
  end; 
  
  //skryt� ukazatele pr�b�hu
  ProgressBar.Visible := false;
end;

//procedura ze vstupn�ch parametr� poskl�d� string pro jdbc.properties a ulo�� k AS
//vstupn� parametry
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

//spu�t�n� akce p�i zm�n� v�b�ru typu datab�ze v comboboxu
procedure ComboBoxOnChange(Sender: TObject);
begin
  //V�b�r datab�ze Postgresql - skryje Instanci 
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
  //V�b�r datab�ze MSSQL - zobraz� Instanci 
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
  //V�b�r embedded datab�ze 
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
//funkce je vol�na p�ed spu�t�n�m instal�toru
function InitializeSetup(): Boolean;
var
  ResultCode: Integer;
begin
  result := true;
  
  //zji�t�n�, zda je ji� aplikace nainstalov�na
  //pokud ano, porovn�v� se verze aplikace a vrac� se v�sledek v prom�nn� IsUpgrade
  if (GetAppVersionString() <> '') and (Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')) <> '') then
  begin
    IsUpgrade := (Trim (GetAppVersionString()) <> Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')));
  end
  else
  begin
    IsUpgrade := false;
  end;
  
  //pokud p�edchoz� kontrola vr�tila, �e se nejden� o aktualizaci a aplikace je ji� nainstalov�na
  if (IsUpgrade = false) and (Trim (GetUninstallString()) <> '') then
  begin 
    //je nainstalovan� shodn� verze aplikace, p�epne se na upgrade
    IsUpgrade := true;
    result := true; 
  end;
end;

//procedura je vol�na p�i zm�n� obrazovky
procedure CurPageChanged(CurPageID: Integer);
var ResultCode, i:integer;
    Text, TextService: String;
begin
  
  i := 0;
  //pokud se jedn� o aktualizaci
  if IsUpgrade then
  begin
    //dopln�n� textu do p��pravy instalace - aktualizace
    if CurPageID=wpReady then
    begin
     Wizardform.ReadyMemo.Lines.Add('');
     Wizardform.ReadyMemo.Lines.Add(_AktualizaceApp);
     Wizardform.ReadyMemo.Lines.Add('      '+ GetAppVersionString()+' -> '+Trim (ExpandConstant('{#emit SetupSetting("AppVersion")}')));  
    end;
    //akce proveden� p�ed spu�t�n�m instalace - aktualizace 
    //zastav� se slu�ba a sma�e adres�� lib  
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
      LabelOdinstalace.Caption := 'Prov�d� se zastaven� slu�by aplikace..'+ #13#10 + #13#10 + 
                                  'Po zastaven� slu�by dojde ke smaz�n� soubor� aplikace a nahr�n� nov�ch. Na z�v�r se provede spu�t�n� slu�by.';
      LabelOdinstalace.Parent := WizardForm;
      WizardForm.Refresh;
      
      Exec('net', 'stop ' + NazevSluzby (''),'', SW_HIDE, ewWaitUntilTerminated, ResultCode);

      sleep (7000);

      FileCopy(ExpandConstant('{app}\apache-tomcat\conf\server.xml'),ExpandConstant('{tmp}\server.xml'), false)

      //sma�e adres��e tomcat
      DelTree(ExpandConstant('{app}\apache-tomcat\bin'), True, True, True);  
      DelTree(ExpandConstant('{app}\apache-tomcat\lib'), True, True, True);  
      DelTree(ExpandConstant('{app}\apache-tomcat\temp'), True, True, True);  

      //sma�e adres�� s aplikac�
      DelTree(ExpandConstant('{app}\apache-tomcat\webapps\'), True, True, True);
      LabelOdinstalace.Visible := false;
    end;
  end; 
  
  //akce proveden� na str�nce dokon�it
  if CurPageID = wpFinished then
  begin
    //zneplatn� tla��tko zp�t
    WizardForm.BackButton.Enabled := False;
    //do�asn� zneplatn� tla��tko dokon�it, do doby ne� se provedou v�echny akce   
    Wizardform.NextButton.Enabled := false;
    
    WizardForm.FinishedLabel.Caption := '';
    

    PortAplikace := EditAppPort.Text;

    //vytvo�� objekt se seznamem �kol�
    SeznamUkolu := TNewCheckListBox.Create(WizardForm);
    SeznamUkolu.Top := WizardForm.FinishedLabel.Top ;
    SeznamUkolu.Left := WizardForm.FinishedLabel.Left;
    SeznamUkolu.Width := WizardForm.Width - SeznamUkolu.Left - ScaleX(20);
    SeznamUkolu.Height := ScaleY(205);
    SeznamUkolu.ShowLines := true;
    SeznamUkolu.Enabled := false;
    SeznamUkolu.Parent := WizardForm;


    //checkbox pro volbu zda se m� zobrazit str�nka aplikace
    CheckBoxZobrazStranku := TNewCheckBox.Create(WizardForm);
    CheckBoxZobrazStranku.Top := SeznamUkolu.Top + SeznamUkolu.Height + ScaleY(8);
    CheckBoxZobrazStranku.Left := WizardForm.FinishedLabel.Left;
    CheckBoxZobrazStranku.Width := WizardForm.Width;
    CheckBoxZobrazStranku.ParentBackground := true;
    CheckBoxZobrazStranku.ParentColor := true;
    CheckBoxZobrazStranku.Color := clWhite;
    CheckBoxZobrazStranku.Caption := 'Po dokon�en� instalace zobrazit str�nku aplikace.';
    CheckBoxZobrazStranku.Checked := True;
    CheckBoxZobrazStranku.Visible := False;
    CheckBoxZobrazStranku.Checked := false;
    CheckBoxZobrazStranku.Parent := WizardForm;

    //Pokud nejde o aktualizaci
    if IsUpgrade = false then
    begin
      //Nahrad� p�eddefinovan� texty hodnotami z instalace
      SeznamUkolu.AddCheckBox(_UlozeniNastaveniApp, '', 0, false, false, false, false, nil);
      NahradHodnotuVSouboru('{app}\elza.url','<urlAplikace>',ExpandConstant('http://' + '{computername}' + ':' + PortAplikace)); //vytvo�� odkaz do start menu
      NahradHodnotuVSouboru('{app}\start.bat','<NazevSluzby>',NazevSluzby ('')); //nastav� n�zev slu�by do d�vky pro start slu�by
      NahradHodnotuVSouboru('{app}\stop.bat','<NazevSluzby>',NazevSluzby ('')); //nastav� n�zev slu�by do d�vky pro vypnut� slu�by
      
      TextService := 'set CATALINA_HOME=' + _Enter + 'set "JAVA_HOME='  + _Enter + '"set JRE_HOME=%cd%\..\jre"' + _Enter + 'setlocal'
      NahradHodnotuVSouboru('{app}\apache-tomcat\bin\service.bat','setlocal',TextService); //nastav� lok�ln� JRE
      NahradHodnotuVSouboru('{app}\apache-tomcat\conf\server.xml','8080',PortAplikace); //nastav� port aplikace
                                     
      SeznamUkolu.Checked[i] := true;
      i := i + 1;

      //pokud nen� nastaveno p�esko�it kontrolu, ulo�� se jdbc do souboru
      if CheckBOx.Checked = false then
      begin
        SeznamUkolu.AddCheckBox(_UlozeniNastaveniPripojeni, '', 0, false, false, false, false, nil);
        UlozJdbcDoSouboruProperties(ComboTypDatabaze.ItemIndex,EditUrlServeru.Text,EditPort.Text,EditInstance.Text,EditNazevDB.Text,EditJmeno.Text,EditHeslo.Text);
        SeznamUkolu.Checked[i] := true;
        i := i + 1;
      end;
      
      //zaregistruje se slu�ba v syst�mu
      SeznamUkolu.AddCheckBox(_RegistaceSluzby, '', 0, false, false, false, false, nil);
      Exec(ExpandConstant('{app}\apache-tomcat\bin\service.bat'), 'install ' + NazevSluzby (''), '',SW_HIDE, ewWaitUntilTerminated, ResultCode);
      SeznamUkolu.Checked[i] := true;
      i := i + 1;
      
    end; 

    //Pokud jde o aktualizaci, vr�t� se z�lohovan� server.xml (z�loha se provede na za��tku instalace)
    if IsUpgrade then
    begin
        FileCopy(ExpandConstant('{tmp}\server.xml'), ExpandConstant('{app}\apache-tomcat\conf\server.xml'), false);
    end;
    
    //sma�e pomocn� soubory
    SeznamUkolu.AddCheckBox(_OdstanovaniDocasnychSouboru, '', 0, false, false, false, false, nil);
    DelTree(ExpandConstant('{app}\TestDb\'), True, True, True);
    SeznamUkolu.Checked[i] := true;
    i := i + 1;
    
    if CheckBOx.Checked = false then
    begin
      SeznamUkolu.AddCheckBox('Start slu�by (operace m��e trvat del�� dobu)', '', 0, false, false, false, false, nil);
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
      
      //Informace o p��stupu do aplikace 
      SeznamUkolu.AddCheckBox('Pro p��stup do aplikace je mo�n� vyu��t u�ivatelsk� jm�no "admin" s heslem "admin".', '', 0, true, false, false, false, nil);
      SeznamUkolu.AddCheckBox('Pro spr�vnou funkci aplikace je pot�eba prov�st import bal��k� pravidel a osob. Zdrojov� soubory se nach�z� v adres��i import.', '', 0, true, false, false, false, nil);
    end
    else
    begin
      SeznamUkolu.AddCheckBox('Pro spu�t�n� aplikace je pot�eba ru�n� nastavit p�ipojen� k datab�zi v souboru elza.yaml a spustit slu�bu.', '', 0, true, false, false, false, nil);
    end;

    //dokon�en� instalace
    SeznamUkolu.AddCheckBox(_Hotovo, '', 0, true, false, false, false, nil);
    
    //zp��stupn� se tla��tko dokon�it
    Wizardform.NextButton.Enabled := true;
  end;
end;

//otev�e odkaz na nastaven� aplikace ve v�choz�m prohl�e�i
procedure OdkazNastaveni(); 
var ErrorCode: Integer;
begin
  ShellExec('', ExpandConstant('http://' + '{computername}:' + PortAplikace), '', '', SW_SHOW, ewNoWait, ErrorCode);
end;

//funkce na p�eskon�en� obrazovek
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  if IsUpgrade then
  begin
    case PageID of
      //seznam str�nek, kter� budou p�esko�eny pokud se jedn� o aktualizaci
      wpLicense: Result := True;
      wpPreparing: Result := True;
      wpInstalling: Result := True;
      wpNastaveniDB: Result := true;
    else
      Result := False;
    end;
  end;
end;

//akce proveden� po kliknut� na tla��tko dal��
function NextButtonClick(CurPageID: Integer): Boolean;
var ResultCode:integer;
    VysledekTestu:String;
    
begin
  //kontrola cesty instalace - nesm� obsahovat mezery
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
  //kliknut� na tla��tko dal�� na formul��i s nastaven�m db
  else if CurPageID = wpNastaveniDB then
  begin
    //pokud je za�krtnut checkbox, p�esko�� sekontrola db a pokra�uje se d�l
    if CheckBox.Checked then
    begin
      result := true;
    end
    //provede se kontrola db, pokud dojde k chyb�, nepokra�uje se
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
  //kliknut� na tal��tko dokon�it
  else if CurPageID = wpFinished then
  begin
    //otev�e odkaz na aplikaci ve v�choz�m prohl�e�i
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

//definice u�ivatelsk�ch obrazovek
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

  Page := CreateCustomPage(wpInfoAfter, 'Konfigurace aplikace', 'Je nutn� nastaven� portu aplikace pod kter�m bude k dispozici. D�le je nutn� vyplnit �daje pro p�ipojen� k datab�zi.');
  
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
  LabelTypDatabaze.Caption := 'Typ datab�ze:';
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
  ComboTypDatabaze.Items.Add('Embedded datab�ze (H2)');
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
  LabelNazevDB.Caption := 'N�zev datab�ze:';
  LabelNazevDB.AutoSize := True;
  LabelNazevDB.Parent := Page.Surface;
  
  EditNazevDB := TNewEdit.Create(Page);
  EditNazevDB.Top := EditUrlServeru.Top + EditUrlServeru.Height + ScaleY(8);
  EditNazevDB.Left := ScaleX(Row1Left);
  EditNazevDB.Width := Page.SurfaceWidth div 2 - ScaleX(26);
  EditNazevDB.Parent := Page.Surface;
  
  LabelJmeno := TNewStaticText.Create(Page);
  LabelJmeno.Top := EditNazevDB.Top + EditNazevDB.Height + ScaleY(11);
  LabelJmeno.Caption := 'Jm�no:';
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
  ButtonTestPripojeniDB.Caption := 'Otestovat p�ipojen� k datab�zi';
  ButtonTestPripojeniDB.OnClick := @ButtonTestPripojeniDBOnClick;
  ButtonTestPripojeniDB.Parent := Page.Surface;
end;

//prvky v li�t� s tla��tky
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
