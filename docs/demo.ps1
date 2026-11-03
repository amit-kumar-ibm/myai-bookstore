<#
.SYNOPSIS
  Step-by-step API walkthrough of the e-bookstore customer journey, for screen recording.

.DESCRIPTION
  Start the app first (see README), then run:   .\docs\demo.ps1
  Each step prints the request, calls the API and shows the JSON response, then waits for Enter
  so you can narrate. Use -NoPause to run straight through.
#>
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [switch]$NoPause
)

$ErrorActionPreference = 'Stop'
$script:n = 0

function Show-Step([string]$title, [string]$request) {
    $script:n++
    Write-Host ""
    Write-Host ("=== STEP {0}: {1}" -f $script:n, $title) -ForegroundColor Cyan
    Write-Host ("    {0}" -f $request) -ForegroundColor Yellow
}

function Show-Json($obj) {
    $obj | ConvertTo-Json -Depth 6 | Write-Host
    if (-not $NoPause) { Read-Host "`n[Enter] for next step" | Out-Null }
}

function Call([string]$method, [string]$path, $body = $null, $headers = @{}) {
    $p = @{ Method = $method; Uri = "$BaseUrl$path"; Headers = $headers; ContentType = 'application/json' }
    if ($null -ne $body) { $p.Body = ($body | ConvertTo-Json -Depth 5) }
    Invoke-RestMethod @p
}

function Expect-Status([int]$code, [string]$method, [string]$path, $headers = @{}) {
    try {
        Invoke-RestMethod -Method $method -Uri "$BaseUrl$path" -Headers $headers | Out-Null
        Write-Host "Unexpected success" -ForegroundColor Red
    } catch {
        $actual = [int]$_.Exception.Response.StatusCode
        $colour = if ($actual -eq $code) { 'Green' } else { 'Red' }
        Write-Host "HTTP $actual (expected $code)" -ForegroundColor $colour
    }
    if (-not $NoPause) { Read-Host "`n[Enter] for next step" | Out-Null }
}

# ---------------------------------------------------------------- Guest browsing
Show-Step 'Guest opens the landing page' 'GET /api/home'
$homePage = Call GET '/api/home'
Show-Json ([pscustomobject]@{
    bestsellers = $homePage.bestsellers.title
    newLaunches = $homePage.newLaunches.title
    recommended = $homePage.recommended.title
})

Show-Step 'Select the category of products' 'GET /api/categories'
$cats = Call GET '/api/categories'
Show-Json ($cats | Select-Object -First 8)
$selfHelp = ($cats | Where-Object name -eq 'Self-help').id

Show-Step 'Browse the brands (publishers) in that category' "GET /api/publishers?categoryId=$selfHelp"
Show-Json (Call GET "/api/publishers?categoryId=$selfHelp")

Show-Step 'Browse products of one brand in the category' "GET /api/books?categoryId=$selfHelp&publisher=Insight Press&sort=priceAsc"
$page = Call GET "/api/books?categoryId=$selfHelp&publisher=Insight%20Press&sort=priceAsc"
Show-Json ($page.content | Select-Object id, title, author, price, tentativeDeliveryDate)

Show-Step 'Search by text' 'GET /api/books?q=minimalism'
$found = Call GET '/api/books?q=minimalism'
Show-Json ($found.content | Select-Object id, title, author, price)
$joyId = $found.content[0].id

Show-Step 'Select the product: tentative delivery date and related reads' "GET /api/books/$joyId"
$detail = Call GET "/api/books/$joyId"
Show-Json ([pscustomobject]@{
    book         = $detail.book | Select-Object title, author, publisher, price, rating, tentativeDeliveryDate
    relatedReads = $detail.relatedBooks.title
})

Show-Step 'Guest cannot use the basket (needs login)' 'GET /api/cart  (no token)'
Expect-Status 401 GET '/api/cart'

# ---------------------------------------------------------------- Login
$email = "demo.$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@example.com"
$demoPass = 'Aa1!' + [guid]::NewGuid().ToString('N')
Show-Step 'Register a new user' 'POST /api/auth/register'
$reg = Call POST '/api/auth/register' @{ email = $email; password = $demoPass; name = 'Demo User' }
Show-Json ([pscustomobject]@{ user = $reg.user; tokenType = $reg.tokenType; expiresInSeconds = $reg.expiresInSeconds })

Show-Step 'Log in (user authentication)' 'POST /api/auth/login'
$login = Call POST '/api/auth/login' @{ email = $email; password = $demoPass }
$auth = @{ Authorization = "Bearer $($login.token)" }
Show-Json ([pscustomobject]@{ user = $login.user; tokenType = $login.tokenType })

Show-Step 'A wrong password is rejected' 'POST /api/auth/login (bad password)'
try {
    Call POST '/api/auth/login' @{ email = $email; password = 'wrong-password' } | Out-Null
} catch {
    Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) (expected 401)" -ForegroundColor Green
}
if (-not $NoPause) { Read-Host "`n[Enter] for next step" | Out-Null }

# ---------------------------------------------------------------- Basket
$pathId = (Call GET '/api/books?q=path%20to%20success').content[0].id

Show-Step 'Add the product to the basket' "POST /api/cart/items  { bookId: $joyId, quantity: 1 }"
$null = Call POST '/api/cart/items' @{ bookId = $joyId; quantity = 1 } $auth
Write-Host "added Joy of Minimalism"

Show-Step 'Add a second product' "POST /api/cart/items  { bookId: $pathId, quantity: 1 }"
$cart = Call POST '/api/cart/items' @{ bookId = $pathId; quantity = 1 } $auth
Show-Json ([pscustomobject]@{
    items = $cart.items | ForEach-Object { [pscustomobject]@{ id = $_.id; title = $_.book.title; qty = $_.quantity; lineTotal = $_.lineTotal } }
    subtotal = $cart.subtotal; tax = $cart.tax; total = $cart.total
    tentativeDeliveryDate = $cart.tentativeDeliveryDate
})

Show-Step 'Change a quantity, then set it back (validation: max 20, min 1)' "PATCH /api/cart/items/$($cart.items[0].id)  { quantity: 2 }"
$cart = Call PATCH "/api/cart/items/$($cart.items[0].id)" @{ quantity = 2 } $auth
Show-Json ([pscustomobject]@{ quantities = $cart.items.quantity; total = $cart.total })
$cart = Call PATCH "/api/cart/items/$($cart.items[0].id)" @{ quantity = 1 } $auth

# ---------------------------------------------------------------- Payment and purchase
Show-Step 'Save the delivery address' 'POST /api/addresses'
$addr = Call POST '/api/addresses' @{
    firstName = 'Demo'; lastName = 'User'; line1 = '12 MG Road'; city = 'Bengaluru'
    state = 'Karnataka'; pin = '560001'; country = 'India'; phone = '+919876543210'
} $auth
Show-Json $addr

Show-Step 'Redeeming points you do not have is rejected' 'POST /api/orders  { giftPointsToRedeem: 50 }'
try {
    Call POST '/api/orders' @{ addressId = $addr.id; paymentMethod = 'UPI'; giftPointsToRedeem = 50 } $auth | Out-Null
} catch {
    Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) (expected 400)" -ForegroundColor Green
}
if (-not $NoPause) { Read-Host "`n[Enter] for next step" | Out-Null }

Show-Step 'Initiate payment and confirm the purchase' 'POST /api/orders  { paymentMethod: CREDIT_CARD }'
$order = Call POST '/api/orders' @{ addressId = $addr.id; paymentMethod = 'CREDIT_CARD'; giftPointsToRedeem = 0 } $auth
Show-Json ([pscustomobject]@{
    id = $order.id; status = $order.status; paymentStatus = $order.paymentStatus
    paymentReference = $order.paymentReference
    items = $order.items.title
    subtotal = $order.subtotal; tax = $order.tax; discount = $order.discount; total = $order.total
    giftPointsEarned = $order.giftPointsEarned
    estimatedDelivery = $order.estimatedDelivery; cancellableUntil = $order.cancellableUntil
})

Show-Step 'Basket is now empty and gift points were earned' 'GET /api/cart   and   GET /api/me'
Show-Json ([pscustomobject]@{ cartItems = (Call GET '/api/cart' $null $auth).items.Count; profile = (Call GET '/api/me' $null $auth) })

# ---------------------------------------------------------------- Order history
Show-Step 'Order history' 'GET /api/orders'
Show-Json ((Call GET '/api/orders' $null $auth) | Select-Object id, status, total, estimatedDelivery)

Show-Step 'Buy In Again: previously purchased books' 'GET /api/orders/buy-again'
Show-Json ((Call GET '/api/orders/buy-again' $null $auth) | Select-Object id, title, price)

Show-Step 'Recommendations based on order history (landing page, logged in)' 'GET /api/home'
Show-Json ((Call GET '/api/home' $null $auth).recommended | ForEach-Object { [pscustomobject]@{ title = $_.title; category = $_.category.name } })

# ---------------------------------------------------------------- Gift points + cancel
Show-Step 'Second purchase, redeeming gift points (1 point = Rs 1)' 'POST /api/orders  { giftPointsToRedeem: 5 }'
$null = Call POST '/api/cart/items' @{ bookId = $joyId; quantity = 2 } $auth
$order2 = Call POST '/api/orders' @{ addressId = $addr.id; paymentMethod = 'WALLET'; giftPointsToRedeem = 5 } $auth
Show-Json ([pscustomobject]@{
    id = $order2.id; subtotal = $order2.subtotal; tax = $order2.tax; discount = $order2.discount
    total = $order2.total; giftPointsRedeemed = $order2.giftPointsRedeemed; giftPointsEarned = $order2.giftPointsEarned
})

Show-Step 'Cancel an order within 48 hours' "POST /api/orders/$($order2.id)/cancel"
$cancelled = Call POST "/api/orders/$($order2.id)/cancel" $null $auth
Show-Json ([pscustomobject]@{ id = $cancelled.id; status = $cancelled.status; paymentStatus = $cancelled.paymentStatus })

Show-Step 'Cancelling again is rejected' "POST /api/orders/$($order2.id)/cancel"
try {
    Call POST "/api/orders/$($order2.id)/cancel" $null $auth | Out-Null
} catch {
    Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) (expected 409)" -ForegroundColor Green
}

Write-Host ""
Write-Host "Demo complete." -ForegroundColor Green
