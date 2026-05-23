[xml]$vn = Get-Content -Raw -Encoding UTF8 'app\src\main\res\values\strings.xml'
[xml]$en = Get-Content -Raw -Encoding UTF8 'app\src\main\res\values-en\strings.xml'

$vnKeys = $vn.resources.string | Select-Object -ExpandProperty name | Sort-Object -Unique
$enKeys = $en.resources.string | Select-Object -ExpandProperty name | Sort-Object -Unique

$missingInEn = Compare-Object $vnKeys $enKeys | Where-Object SideIndicator -eq '<=' | Select-Object -ExpandProperty InputObject
$missingInVn = Compare-Object $vnKeys $enKeys | Where-Object SideIndicator -eq '=>' | Select-Object -ExpandProperty InputObject

Write-Output "Missing in EN:"
$missingInEn

Write-Output "Missing in VN:"
$missingInVn
