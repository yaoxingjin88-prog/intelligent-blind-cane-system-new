# 本机量化压测：看板冷热/并发、监控总览、AI 限流
# 用法（后端已启动）: powershell -File backend/scripts/bench_metrics.ps1

param(
    [string]$BaseUrl = "http://127.0.0.1:8081",
    [string]$Username = "admin",
    [string]$Password = "123456",
    [int]$Concurrency = 8
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-TimedGet {
    param([string]$Url, [hashtable]$Headers)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $resp = Invoke-WebRequest -Uri $Url -Headers $Headers -Method GET -UseBasicParsing -TimeoutSec 60
        $sw.Stop()
        return [pscustomobject]@{
            Ok       = $true
            Status   = [int]$resp.StatusCode
            Ms       = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
            Bytes    = $resp.RawContentLength
        }
    } catch {
        $sw.Stop()
        $status = 0
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        return [pscustomobject]@{
            Ok     = $false
            Status = $status
            Ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
            Bytes  = 0
            Error  = $_.Exception.Message
        }
    }
}

function Invoke-TimedPostJson {
    param([string]$Url, [hashtable]$Headers, [string]$Body)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $resp = Invoke-WebRequest -Uri $Url -Headers $Headers -Method POST -Body $Body -ContentType "application/json; charset=utf-8" -UseBasicParsing -TimeoutSec 15
        $sw.Stop()
        return [pscustomobject]@{
            Ok     = $true
            Status = [int]$resp.StatusCode
            Ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
        }
    } catch {
        $sw.Stop()
        $status = 0
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode.value__
            if (-not $status) { $status = [int]$_.Exception.Response.StatusCode }
        }
        return [pscustomobject]@{
            Ok     = $false
            Status = $status
            Ms     = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
            Error  = $_.Exception.Message
        }
    }
}

Write-Host "=== 1) 登录获取 Token ===" -ForegroundColor Cyan
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/login" -Method POST -Body $loginBody -ContentType "application/json; charset=utf-8"
if (-not $loginResp.data.token) {
    throw "登录失败: $($loginResp | ConvertTo-Json -Compress)"
}
$token = $loginResp.data.token
$headers = @{ Authorization = "Bearer $token" }
Write-Host "登录成功"

Write-Host "`n=== 2) 看板冷/热耗时 (/api/analytics/dashboard) ===" -ForegroundColor Cyan
# 提示：若服务刚处理过看板，热数据可能仍在 30s TTL 内；冷测前可重启后端
$cold = Invoke-TimedGet -Url "$BaseUrl/api/analytics/dashboard" -Headers $headers
Start-Sleep -Milliseconds 200
$hot1 = Invoke-TimedGet -Url "$BaseUrl/api/analytics/dashboard" -Headers $headers
$hot2 = Invoke-TimedGet -Url "$BaseUrl/api/analytics/dashboard" -Headers $headers
Write-Host ("cold:  status={0} ms={1}" -f $cold.Status, $cold.Ms)
Write-Host ("hot1:  status={0} ms={1}" -f $hot1.Status, $hot1.Ms)
Write-Host ("hot2:  status={0} ms={1}" -f $hot2.Status, $hot2.Ms)
if ($cold.Ms -gt 0 -and $hot1.Ms -gt 0) {
    $speedup = [math]::Round($cold.Ms / [math]::Max($hot1.Ms, 0.1), 1)
    Write-Host ("冷/热加速比约 {0}x（仅供本机参考）" -f $speedup)
}

Write-Host "`n=== 3) 看板并发 $($Concurrency) 路 ===" -ForegroundColor Cyan
$jobs = @()
for ($i = 0; $i -lt $Concurrency; $i++) {
    $jobs += Start-Job -ScriptBlock {
        param($u, $t)
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $r = Invoke-WebRequest -Uri $u -Headers @{ Authorization = "Bearer $t" } -UseBasicParsing -TimeoutSec 60
            $sw.Stop()
            [pscustomobject]@{ Status = [int]$r.StatusCode; Ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 1) }
        } catch {
            $sw.Stop()
            $st = 0
            if ($_.Exception.Response) { $st = [int]$_.Exception.Response.StatusCode }
            [pscustomobject]@{ Status = $st; Ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 1) }
        }
    } -ArgumentList "$BaseUrl/api/analytics/dashboard", $token
}
$conc = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force
$ok = @($conc | Where-Object { $_.Status -eq 200 }).Count
$msList = @($conc | ForEach-Object { $_.Ms } | Sort-Object)
$p50 = $msList[[math]::Floor(($msList.Count - 1) * 0.5)]
$p95 = $msList[[math]::Floor(($msList.Count - 1) * 0.95)]
$max = ($msList | Measure-Object -Maximum).Maximum
Write-Host ("并发成功 {0}/{1}  P50={2}ms  P95={3}ms  max={4}ms" -f $ok, $Concurrency, $p50, $p95, $max)
Write-Host "说明: single-flight 需结合后端日志确认同一时刻仅构建 1 次；单元测试已覆盖 getAllDevices 只调用 1 次。"

Write-Host "`n=== 4) 监控总览冷/热 (/api/monitor/overview) ===" -ForegroundColor Cyan
$mc = Invoke-TimedGet -Url "$BaseUrl/api/monitor/overview" -Headers $headers
Start-Sleep -Milliseconds 150
$mh = Invoke-TimedGet -Url "$BaseUrl/api/monitor/overview" -Headers $headers
Write-Host ("monitor cold: status={0} ms={1}" -f $mc.Status, $mc.Ms)
Write-Host ("monitor hot:  status={0} ms={1}" -f $mh.Status, $mh.Ms)

Write-Host "`n=== 5) AI 限流 (默认 20次/分钟，打 25 次) ===" -ForegroundColor Cyan
# 用非法短 body，尽量在限流放行后快速失败，避免打爆 DeepSeek
$aiBody = '{"message":""}'
$aiHeaders = @{
    Authorization = "Bearer $token"
}
$codes = @{}
$rateLimited = 0
$passed = 0
for ($i = 1; $i -le 25; $i++) {
    $r = Invoke-TimedPostJson -Url "$BaseUrl/api/ai/chat" -Headers $aiHeaders -Body $aiBody
    $code = $r.Status
    if (-not $codes.ContainsKey([string]$code)) { $codes[[string]$code] = 0 }
    $codes[[string]$code]++
    if ($code -eq 429) { $rateLimited++ } else { $passed++ }
    Write-Host ("  #{0:D2} status={1} ms={2}" -f $i, $code, $r.Ms)
}
Write-Host ("放行(非429)={0}  限流429={1}" -f $passed, $rateLimited)
Write-Host ("状态码分布: {0}" -f (($codes.GetEnumerator() | ForEach-Object { "$($_.Key)x$($_.Value)" }) -join ", "))

Write-Host "`n=== 简历可用结论（填入实测数字） ===" -ForegroundColor Green
Write-Host ("- 看板冷请求约 {0}ms，缓存命中约 {1}ms" -f $cold.Ms, $hot1.Ms)
Write-Host ("- {0} 并发看板全部成功时 P50≈{1}ms / P95≈{2}ms（single-flight 见单测/日志）" -f $Concurrency, $p50, $p95)
Write-Host ("- 监控总览冷≈{0}ms / 热≈{1}ms（10s TTL）" -f $mc.Ms, $mh.Ms)
Write-Host ("- AI 接口 25 次请求中出现 429={0} 次（配置 20/min）" -f $rateLimited)
