# No Maven required. Windows PowerShell + JDK 17+
$root = Resolve-Path "$PSScriptRoot/.."
$tmp = Join-Path $env:TEMP ("smarthome-engine-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
    $files = @(Get-ChildItem "$root/backend/src/main/java/com/smarthome/smart_home_backend/ds" -Recurse -Filter '*.java' | ForEach-Object FullName)
    $files += "$root/scripts/EngineSmokeTest.java"
    & javac -encoding UTF-8 -d $tmp $files
    if ($LASTEXITCODE -ne 0) { throw 'javac compilation failed' }
    & java -cp $tmp EngineSmokeTest
    if ($LASTEXITCODE -ne 0) { throw 'Engine smoke test failed' }
} finally { Remove-Item $tmp -Force -Recurse -ErrorAction SilentlyContinue }
