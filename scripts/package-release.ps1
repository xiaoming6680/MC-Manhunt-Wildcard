param([string]$Version = '')
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (-not $Version) {
    $Version = ((Get-Content (Join-Path $repo 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }) -split '=', 2)[1].Trim()
}
if ($Version -notmatch '^\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?$') { throw 'Invalid release version' }
$targets = @(Get-Content (Join-Path $PSScriptRoot 'minecraft-targets.json') -Raw | ConvertFrom-Json | ForEach-Object {
    $game = $_.minecraft
    $prefix = if ($_.project -eq '.') { '' } else { $_.project + '/' }
    $build = if ($_.project -match '^versions/1\.21\.') { "build/$game" } else { 'build' }
    $archive = if ($_.project -eq '.') { "MC-Manhunt-Wildcard-$Version.jar" } else { "MC-Manhunt-Wildcard-$game-$Version.jar" }
    @{ Game = $game; Source = "$prefix$build/libs/$archive" }
})
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Validate every input before writing the release staging directory.
foreach ($target in $targets) {
    $source = Join-Path $repo $target.Source
    $zip = [IO.Compression.ZipFile]::OpenRead($source)
    try {
        $reader = [IO.StreamReader]::new($zip.GetEntry('fabric.mod.json').Open())
        try { $metadata = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($metadata.id -ne 'hunterwildcard' -or $metadata.version -ne $Version -or $metadata.depends.minecraft -ne $target.Game) {
            throw "Wrong mod/version/Minecraft metadata in $source"
        }
        if ($zip.Entries.FullName -match '^com/xiaoming/hunterwildcard/test/') {
            throw "Test classes must not be included in $source"
        }
    } finally { $zip.Dispose() }
}
$destination = Join-Path $repo "dist/$Version"
New-Item -ItemType Directory -Force -Path $destination | Out-Null
$hashes = foreach ($target in $targets) {
    $name = "MC-Manhunt-Wildcard-$Version-mc$($target.Game).jar"
    $output = Join-Path $destination $name
    Copy-Item -LiteralPath (Join-Path $repo $target.Source) -Destination $output -Force
    "$((Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash.ToLowerInvariant())  $name"
}
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText((Join-Path $destination 'SHA256SUMS.txt'), ($hashes -join "`n") + "`n", $utf8)
Copy-Item -LiteralPath (Join-Path $repo "RELEASE_NOTES_$Version.md") -Destination $destination -Force
Write-Output "Validated and packaged all $($targets.Count) Fabric builds in $destination"
