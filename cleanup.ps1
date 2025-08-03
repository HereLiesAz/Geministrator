<#
.SYNOPSIS
    A PowerShell script to find all misplaced Kotlin files within the project,
    keep the older version of any duplicates, move it to the correct
    Gradle source directory, and then clean up empty directories.
.DESCRIPTION
    This script is the PowerShell equivalent of the cleanup.sh bash script,
    designed to run on Windows systems.
#>

# --- WARNING ---
# This script performs destructive file operations (Move-Item and Remove-Item).
# It is highly recommended to run this in 'dry-run' mode first.
# ALWAYS make sure your project is committed to Git before running this script.
# --- WARNING ---

# --- Configuration ---
$SrcDir = "src\main\kotlin"
$PSScriptRoot = Get-Location

# --- Interactive User Prompt ---
$choice = Read-Host "Run in DRY-RUN mode (y/n)? (No files will be changed)"
if ($choice -match '^[Yy]') {
    $DryRun = $true
    Write-Host "--- RUNNING IN DRY-RUN MODE ---" -ForegroundColor Yellow
    Write-Host "No files will be moved or deleted. Commands that would be run will be printed instead."
} else {
    $DryRun = $false
    Write-Host "--- RUNNING IN LIVE MODE ---" -ForegroundColor Red
    Write-Host "File operations will be performed."
}
Write-Host "---"

# --- Main Logic ---

# Function to process a single module
function Process-Module {
    param (
        [string]$modulePath
    )
    Write-Host "Processing module: $modulePath"

    # Find ALL Kotlin files in the module, but exclude those already in a correct source directory.
    Get-ChildItem -Path $modulePath -Filter "*.kt" -Recurse | Where-Object { $_.FullName -notlike "*\$SrcDir*" } | ForEach-Object {
        $misplacedFile = $_
        $filename = $misplacedFile.Name

        # Read package name directly from the file
        $packageLine = Get-Content $misplacedFile.FullName | Select-String -Pattern '^package ' | Select-Object -First 1
        if (-not $packageLine) {
            Write-Warning "  ! Could not find package declaration in $($misplacedFile.FullName). Skipping."
            return
        }

        $packagePath = ($packageLine.Line -split ' ')[1].Replace('.', '\')
        $correctPath = Join-Path -Path $modulePath -ChildPath "$SrcDir\$packagePath\$filename"

        # Check if a file already exists at the correct destination
        if (Test-Path $correctPath) {
            Write-Host "  Found duplicate: $filename (at $($misplacedFile.FullName))"
            $correctFile = Get-Item $correctPath

            # Compare modification times to find the older file
            if ($misplacedFile.LastWriteTime -lt $correctFile.LastWriteTime) {
                Write-Host "    -> Keeping the misplaced version (it's older)."
                if ($DryRun) {
                    Write-Host "    DRY-RUN: Would delete: $($correctFile.FullName)" -ForegroundColor Cyan
                    Write-Host "    DRY-RUN: Would move: $($misplacedFile.FullName) -> $($correctFile.FullName)" -ForegroundColor Cyan
                } else {
                    Write-Host "    DELETING: $($correctFile.FullName)" -ForegroundColor Magenta
                    Remove-Item $correctFile.FullName
                    Write-Host "    MOVING: $($misplacedFile.FullName) -> $($correctFile.FullName)" -ForegroundColor Green
                    Move-Item -Path $misplacedFile.FullName -Destination $correctFile.FullName
                }
            } else {
                Write-Host "    -> Keeping the version in the correct src path (it's older or same age)."
                if ($DryRun) {
                    Write-Host "    DRY-RUN: Would delete: $($misplacedFile.FullName)" -ForegroundColor Cyan
                } else {
                    Write-Host "    DELETING: $($misplacedFile.FullName)" -ForegroundColor Magenta
                    Remove-Item $misplacedFile.FullName
                }
            }
        } else {
            Write-Host "  Found misplaced file: $($misplacedFile.FullName)"
            $correctDir = Split-Path -Path $correctPath -Parent
            if ($DryRun) {
                Write-Host "    DRY-RUN: Would create directory: $correctDir" -ForegroundColor Cyan
                Write-Host "    DRY-RUN: Would move: $($misplacedFile.FullName) -> $correctPath" -ForegroundColor Cyan
            } else {
                Write-Host "    CREATING DIR: $correctDir"
                New-Item -ItemType Directory -Path $correctDir -Force | Out-Null
                Write-Host "    MOVING: $($misplacedFile.FullName) -> $correctPath" -ForegroundColor Green
                Move-Item -Path $misplacedFile.FullName -Destination $correctPath
            }
        }
        Write-Host "---"
    }
}

# --- Execution ---

# 1. Process all modules to move/delete files
$moduleRoots = @("core", "common", "adapters", "products")

foreach ($root in $moduleRoots) {
    if (Test-Path $root) {
        if ($root -eq "adapters" -or $root -eq "products") {
            Get-ChildItem -Path $root -Directory | ForEach-Object { Process-Module $_.FullName }
        } else {
            Process-Module (Join-Path -Path $PSScriptRoot -ChildPath $root)
        }
    }
}

# 2. Clean up empty directories
Write-Host "Cleaning up empty directories..."
if ($DryRun) {
    Write-Host "  DRY-RUN: Would find and delete empty directories."
} else {
    # Get all directories, sort by path length descending (so we process children before parents)
    Get-ChildItem -Path $PSScriptRoot -Recurse -Directory | Sort-Object -Property { $_.FullName.Length } -Descending | ForEach-Object {
        # If a directory has no items in it, delete it
        if (-not (Get-ChildItem -Path $_.FullName)) {
            Write-Host "  Removing empty directory: $($_.FullName)" -ForegroundColor Magenta
            Remove-Item $_.FullName
        }
    }
    Write-Host "  Empty directories removed."
}

if ($DryRun) {
    Write-Host "--- DRY-RUN COMPLETE ---" -ForegroundColor Yellow
} else {
    Write-Host "✅ Cleanup complete." -ForegroundColor Green
}
