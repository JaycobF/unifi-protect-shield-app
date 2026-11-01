; Inno Setup script for the Windows installer.
; Build after PyInstaller:  iscc /DAppVersion=1.0.0 installer\windows\dv-converter.iss
; Paths are relative to this file.

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif
#define AppName "DV Converter"
#define AppExe "DV Converter.exe"
#define BuildDir "..\..\dist\DV Converter"

[Setup]
AppId={{8F3C2A51-6D4E-4B8A-9C1F-2E7D5A0B9C34}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppName}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; Installs for the current user without an admin prompt; users can choose all users instead.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=..\..\dist
OutputBaseFilename=DV-Converter-Setup-{#AppVersion}
SetupIconFile=..\..\assets\icon.ico
UninstallDisplayIcon={app}\{#AppExe}
UninstallDisplayName={#AppName}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
CloseApplications=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[InstallDelete]
; Remove files left by an older version before copying the new ones.
Type: filesandordirs; Name: "{app}\_internal"

[Files]
Source: "{#BuildDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "https://mkvtoolnix.download/downloads.html#windows"; Description: "Download MKVToolNix (DV Converter needs it to save files)"; Flags: postinstall shellexec skipifsilent; Check: NeedsMkvToolNix
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[Code]
function NeedsMkvToolNix: Boolean;
begin
  Result := not (
    FileExists(ExpandConstant('{commonpf64}\MKVToolNix\mkvmerge.exe')) or
    FileExists(ExpandConstant('{commonpf32}\MKVToolNix\mkvmerge.exe')) or
    FileExists(ExpandConstant('{localappdata}\Microsoft\WinGet\Links\mkvmerge.exe')));
end;
