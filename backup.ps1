<#
.SYNOPSIS
    A PowerShell script to back up all project files into a single text document,
    now automated to run from a Git hook.
.DESCRIPTION
    This script is the PowerShell equivalent of the backup.sh bash script,
    designed to run on Windows systems. It creates a full or tree-based backup
    and prunes old backups, keeping only the most recent ones.
#>

# --- Configuration ---
$ScriptName = "backup.ps1"
$BackupPrefix = "project_backup"
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$PSScriptRoot = Get-Location

# --- Automation Settings ---
# Set the backup type: "Full" or "Tree"
$BackupType = "Full" 
# Set the number of recent backups to keep
$KeepLastNBackups = 5

# Directories to exclude from the backup entirely, regardless of nesting level.
$excludeDirs = @(
    ".git",
    ".github", # Exclude GitHub Actions workflows
    ".gradle",
    ".idea",
    ".vscode", # VS Code workspace settings
    "build",
    "node_modules",
    "out"
)

# File patterns to exclude from the backup.
$excludeFiles = @(
    $ScriptName, # This script itself
    "${BackupPrefix}*.txt", # Previous backups
    "*.iml", # IntelliJ module files
    "local.properties", # Local SDK paths and user settings
    "gradlew", # Executable shell scripts
    "gradlew.bat", # Executable batch scripts
    "*.jar", # Java archives (e.g., gradle-wrapper.jar)
    "*.bin", # Binary files
    "package-lock.json", # NPM lock file
    "yarn.lock", # Yarn lock file
    # Common binary/media file types that waste tokens
    "*.png", "*.jpg", "*.jpeg", "*.gif", "*.svg", "*.ico",
    "*.woff", "*.woff2", "*.ttf", "*.eot",
    # OS-specific files
    ".DS_Store", # macOS system files
    "Thumbs.db"                # Windows thumbnail cache
)

# --- Main Logic ---

# This variable must be declared outside the 'if' blocks to be in scope for the cleanup.
$BackupFile = $null

if ($BackupType -eq "Full") {
    # --- Full Content Backup ---
    $BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "${BackupPrefix}_full_${Timestamp}.txt"
    Write-Host "Starting FULL project backup..." -ForegroundColor Green
    Write-Host "Output file will be: $BackupFile"

    Get-ChildItem -Path $PSScriptRoot -Recurse -File | Where-Object {
        $item = $_
        $isExcluded = $false

        # Check against excluded file name patterns.
        foreach ($pattern in $excludeFiles)
        {
            if ($item.Name -like $pattern)
            {
                $isExcluded = $true; break
            }
        }

        # If not excluded by name, check if the item is within an excluded directory path.
        if (!$isExcluded)
        {
            # Normalize the path by adding separators to ensure we match whole directory names.
            # e.g., match `\build\` and not a file like `my_build_script.ps1`
            $normalizedPath = "$( [System.IO.Path]::DirectorySeparatorChar )$( $item.FullName )$( [System.IO.Path]::DirectorySeparatorChar )"
            foreach ($dir in $excludeDirs)
            {
                $patternToFind = "$( [System.IO.Path]::DirectorySeparatorChar )$dir$( [System.IO.Path]::DirectorySeparatorChar )"
                if ( $normalizedPath.Contains($patternToFind))
                {
                    $isExcluded = $true; break
                }
            }
        }

        !$isExcluded
    } | ForEach-Object {
        $file = $_
        $relativePath = $file.FullName.Replace($PSScriptRoot, '.')
        Write-Host "Backing up: $relativePath"

        # Append a header and the content of the file.
        Add-Content -Path $BackupFile -Value "===================================================================="
        Add-Content -Path $BackupFile -Value "FILE: $relativePath"
        Add-Content -Path $BackupFile -Value "===================================================================="
        Add-Content -Path $BackupFile -Value (Get-Content $file.FullName -Raw)
        Add-Content -Path $BackupFile -Value "`r`n"
    }
}
elseif ($BackupType -eq "Tree") {
    # --- File Tree Backup ---
    $BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "${BackupPrefix}_tree_${Timestamp}.txt"
    Write-Host "Starting FILE TREE backup..." -ForegroundColor Green
    Write-Host "Output file will be: $BackupFile"

    Get-ChildItem -Path $PSScriptRoot -Recurse | Where-Object {
        $item = $_
        $isExcluded = $false

        # Check against excluded file name patterns.
        # This will correctly skip directories as their names won't match file patterns.
        foreach ($pattern in $excludeFiles)
        {
            if ($item.Name -like $pattern)
            {
                $isExcluded = $true; break
            }
        }

        # If not excluded by name, check if the item is within an excluded directory path.
        if (!$isExcluded)
        {
            # Normalize the path by adding separators to ensure we match whole directory names.
            # e.g., match `\build\` and not just a file containing "build".
            $normalizedPath = "$( [System.IO.Path]::DirectorySeparatorChar )$( $item.FullName )$( [System.IO.Path]::DirectorySeparatorChar )"
            foreach ($dir in $excludeDirs)
            {
                $patternToFind = "$( [System.IO.Path]::DirectorySeparatorChar )$dir$( [System.IO.Path]::DirectorySeparatorChar )"
                if ( $normalizedPath.Contains($patternToFind))
                {
                    $isExcluded = $true; break
                }
            }
        }

        !$isExcluded
    } | ForEach-Object {
        $relativePath = $_.FullName.Replace($PSScriptRoot, '.')
        Add-Content -Path $BackupFile -Value $relativePath
    }
}
else {
    Write-Error "Invalid BackupType configuration. Please set to 'Full' or 'Tree'."
    exit 1
}

# --- Cleanup Logic ---
Write-Host "---"
Write-Host "Cleaning up old backups..." -ForegroundColor Yellow

# Get all backups, sort by name (which contains the timestamp), newest first
# This is required for GitHub Actions, as CreationTime is not preserved on checkout
$allBackups = Get-ChildItem -Path $PSScriptRoot -Filter "${BackupPrefix}_*.txt" | Sort-Object Name -Descending

# Select all backups *except* the N most recent
$oldBackups = $allBackups | Select-Object -Skip $KeepLastNBackups

if ($oldBackups) {
    foreach ($oldFile in $oldBackups) {
        Write-Host "Removing old backup: $($oldFile.Name)"
        # This removes the file from the local filesystem.
        # The GitHub Action will then commit this deletion.
        Remove-Item -Path $oldFile.FullName -ErrorAction SilentlyContinue
    }
}
else {
    Write-Host "No old backups to remove."
}

Write-Host "---"
Write-Host "✅ Backup complete." -ForegroundColor Green
Write-Host "Latest backup file: $BackupFile"
