Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=(.*)$') {
        $name = $matches[1]
        $value = $matches[2].Trim()

        if ($value.Length -ge 2 -and
            $value.StartsWith('"') -and
            $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}