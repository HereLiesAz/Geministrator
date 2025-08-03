#Requires -Version 5.1

<#
.SYNOPSIS
    Creates a directory structure from a FILETREE file, using the script's location as the root.

.DESCRIPTION
    This script reads a text file named FILETREE that outlines a directory structure.
    It assumes the script is located in the project's root directory. The first line of
    the FILETREE file is expected to be the root directory's name and is ignored.
    The script then creates all subsequent files and folders relative to its own location.

.NOTES
    Author: Gemini
    Version: 5.0
    Last Modified: 2025-08-02

    *** ENCODING NOTE ***
    To prevent character encoding issues in PowerShell, it's best to save this .ps1 file
    with "UTF-8 with BOM" encoding if your editor supports it. This version has been
    modified to be robust against these issues.

.USAGE
    1. Place this script inside your project's root folder (e.g., inside 'gemini-orchestrator/').
    2. Create a file named `FILETREE` (with no extension) in the same directory.
    3. Ensure the first line of FILETREE is the name of the root folder.
    4. Run the script from the PowerShell terminal: .\Create-FileTree.ps1
#>

# --- Script Configuration ---
$fileTreeName = "FILETREE"
$scriptRoot = $PSScriptRoot # The directory where the script is located. THIS IS THE ROOT.

# --- Main Script Logic ---

# Construct the full path to the FILETREE file
$fileTreePath = Join-Path -Path $scriptRoot -ChildPath $fileTreeName

# Check if the FILETREE file exists
if (-not (Test-Path -Path $fileTreePath -PathType Leaf)) {
    Write-Error "Error: The file '$fileTreeName' was not found in the script's directory: $scriptRoot"
    return
}

Write-Host "Reading structure from '$fileTreePath'..."
Write-Host "Using script location as project root: $scriptRoot"

# Read the content of the file tree, specifying UTF8 encoding to handle special characters.
$fileTreeContent = Get-Content -Path $fileTreePath -Encoding UTF8

# Check if the file is empty or has only one line
if ($fileTreeContent.Length -le 1) {
    Write-Warning "Warning: FILETREE contains no items to create."
    return
}

try {
    # This hashtable will keep track of the path at different hierarchy levels.
    # Level 0 is the script's root directory.
    $pathStack = @{ 0 = $scriptRoot }

    # Unicode characters for use in the script
    $charFolder = [char]::ConvertFromUtf32(0x1F4C1)   # 📁
    $charVertical = [char]0x2502 # │

    # --- Process Child Items ---
    # Process all lines *except* for the first one, which is assumed to be the root folder name.
    foreach ($line in ($fileTreeContent | Select-Object -Skip 1)) {
        # Skip empty lines or lines that are purely for visual tree structure
        if ([string]::IsNullOrWhiteSpace($line) -or [string]::IsNullOrWhiteSpace($line.Trim(" $charVertical"))) {
            continue
        }

        # --- New, Robust Parsing Logic ---

        # 1. Use a single regex to parse the line into its constituent parts.
        # This regex identifies the indentation (spaces and vertical bars) and separates it from the name.
        $parserRegex = "^(?<indent>[\s$charVertical]*)(?<structure>[^\w\s\./\\]*)(?<name>.*)$"
        $match = $line | Select-String -Pattern $parserRegex

        if (-not $match) {
            Write-Warning "Could not parse line: '$line'. Skipping."
            continue
        }

        $indentation = $match.Matches[0].Groups['indent'].Value
        $namePart = $match.Matches[0].Groups['name'].Value.Trim()

        # 2. Calculate the hierarchy level based on the indentation length.
        # Each level is considered to be 4 characters wide.
        $level = [int]($indentation.Length / 4) + 1

        # 3. Clean the extracted name part
        if ($namePart.StartsWith($charFolder)) {
            $namePart = $namePart.Substring($charFolder.Length).TrimStart()
        }

        # 4. Determine if the item is a directory or a file
        $isFolder = $line.Contains($charFolder) -or $namePart.EndsWith('/') -or $namePart.EndsWith('\')
        $itemName = $namePart.TrimEnd('/', '\')

        # --- Path Construction ---

        # Get the parent path from the stack based on the calculated level
        $parentPath = $pathStack[$level - 1]

        if ($null -eq $parentPath) {
            Write-Warning "Cannot determine parent path for line: '$line' (Level: $level). The FILETREE might be malformed. Skipping."
            continue
        }

        # Construct the full path for the current item
        $currentItemPath = Join-Path -Path $parentPath -ChildPath $itemName

        # --- File/Folder Creation ---

        if ($isFolder) {
            Write-Host "Creating Directory: $currentItemPath"
            # The -Force switch creates parent directories as needed.
            New-Item -Path $currentItemPath -ItemType Directory -Force -ErrorAction SilentlyContinue | Out-Null
            # Add the new directory path to our stack for any children it may have.
            $pathStack[$level] = $currentItemPath
        }
        else {
            Write-Host "Creating File:      $currentItemPath"
            # Ensure the parent directory exists before creating the file
            $parentDir = Split-Path -Path $currentItemPath -Parent
            if (-not (Test-Path -Path $parentDir)) {
                New-Item -Path $parentDir -ItemType Directory -Force -ErrorAction SilentlyContinue | Out-Null
            }
            if (-not (Test-Path -Path $currentItemPath)) {
                New-Item -Path $currentItemPath -ItemType File -Force | Out-Null
            } else {
                Write-Host "  - File already exists. Skipping."
            }
        }
    }

    Write-Host "`n✅ File tree generation completed successfully."

}
catch {
    Write-Error "An unexpected error occurred: $_"
}