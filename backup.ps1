<#
.SYNOPSIS
    A PowerShell script to back up all project files into a single text document,
    with an option for a full content backup or a file tree only.
.DESCRIPTION
    This script is the PowerShell equivalent of the backup.sh bash script,
    designed to run on Windows systems.
#>

# --- Configuration ---
$ScriptName = "backup.ps1"
$BackupPrefix = "project_backup"
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$PSScriptRoot = Get-Location

# --- User Prompt ---
Write-Host "Please choose a backup type:"
Write-Host "  1) Full Backup (all file contents)"
Write-Host "  2) File Tree Only (a list of all files and directories)"
$choice = Read-Host "Enter your choice (1 or 2)"

# --- Main Logic ---

switch ($choice) {
    "1" {
        # --- Full Content Backup ---
        $BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "${BackupPrefix}_full_${Timestamp}.txt"
        Write-Host "Starting FULL project backup..." -ForegroundColor Green
        Write-Host "Output file will be: $BackupFile"

        # Get all files, excluding this script, previous backups, and common build/dependency directories.
        Get-ChildItem -Path $PSScriptRoot -Recurse -File -Exclude ".git", ".gradle", "build", "node_modules", "out", $ScriptName, "${BackupPrefix}*.txt" | ForEach-Object {
            $file = $_
            Write-Host "Backing up: $($file.FullName.Replace($PSScriptRoot, '.'))"

            # Append a header and the content of the file.
            Add-Content -Path $BackupFile -Value "===================================================================="
            Add-Content -Path $BackupFile -Value "FILE: $($file.FullName.Replace($PSScriptRoot, '.'))"
            Add-Content -Path $BackupFile -Value "===================================================================="
            Add-Content -Path $BackupFile -Value (Get-Content $file.FullName -Raw)
            Add-Content -Path $BackupFile -Value "`r`n"
        }
    }
    "2" {
        # --- File Tree Backup ---
        $BackupFile = Join-Path -Path $PSScriptRoot -ChildPath "${BackupPrefix}_tree_${Timestamp}.txt"
        Write-Host "Starting FILE TREE backup..." -ForegroundColor Green
        Write-Host "Output file will be: $BackupFile"

        # Get all files and directories, excluding common build/dependency directories.
        Get-ChildItem -Path $PSScriptRoot -Recurse -Exclude ".git", ".gradle", "build", "node_modules", "out", $ScriptName, "${BackupPrefix}*.txt" | ForEach-Object {
            # Get the relative path for a cleaner tree view
            $relativePath = $_.FullName.Replace($PSScriptRoot, '.')
            Add-Content -Path $BackupFile -Value $relativePath
        }
    }
    default {
        Write-Error "Invalid choice. Please run the script again and enter 1 or 2."
        exit 1
    }
}

Write-Host "---"
Write-Host "✅ Backup complete." -ForegroundColor Green
Write-Host "Output has been saved to: $BackupFile"
