from gemini import Text
import subprocess

def run_gemini(api_key, command):
    """Runs a Gemini CLI command."""
    try:
        # The gemini-ai-toolkit doesn't have a direct CLI wrapper,
        # so we'll call it as a subprocess.
        # This is a placeholder for a more robust implementation.
        result = subprocess.run(
            ["python", "-m", "gemini", "--api_key", api_key, "--text", "--prompt", command],
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            return result.stdout
        else:
            return f"Error: {result.stderr}"
    except Exception as e:
        return f"An error occurred: {e}"
