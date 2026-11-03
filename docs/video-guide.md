# Video Recording Guide: Book Worm API Walkthrough

A step-by-step script for the video the capstone asks for (slide 11). It shows the AI-assisted
workflow and then the working API. About 10 to 12 minutes.

## Before you press record

1. Check PostgreSQL is running: `Get-Service postgresql-x64-18` should show `Running`.
2. Open a **new** PowerShell window so it picks up `SPRING_DATASOURCE_PASSWORD` and `APP_JWT_SECRET`.
3. Start the app in that window:

   ```powershell
   cd C:\Projects\MyAi
   .\mvnw.cmd spring-boot:run
   ```

   Wait for `Started MyAiApplication`.
4. Open a second PowerShell window for the demo.
5. Open these in your editor so you can show them: `docs/openapi.yaml`, `README.md`,
   `src/main/java/com/myai/`.

If you skip PostgreSQL, run the app with `"-Dspring-boot.run.profiles=dev"` instead. Say so in the
video.

## Part 1: The AI workflow (about 3 minutes)

Narrate these as you show them:

| # | Show | Say |
|---|------|-----|
| 1 | The wireframe slides 5 to 10 | "I gave the wireframes to IBM Bob and asked it to identify the data entities and features." |
| 2 | `docs/openapi.yaml` | "Bob generated the OpenAPI spec from the wireframes. I reviewed it and saved it." |
| 3 | The `model/`, `service/`, `controller/` folders | "Bob generated the Spring Boot code from the spec: entities, repositories, services, controllers." |
| 4 | `application.properties` | "PostgreSQL is configured through environment variables, so no password is in source control." |
| 5 | A terminal running `.\mvnw.cmd test` | "Bob wrote an end-to-end test of the whole customer journey. All tests pass." |
| 6 | Examples of issues Bob found and fixed | "Bob found that the original `pom.xml` could not resolve, and fixed it. It also caught a wrong assumption in my test about recommendations." |

## Part 2: Live API demo (about 6 minutes)

In the second window run:

```powershell
cd C:\Projects\MyAi
powershell -ExecutionPolicy Bypass -File .\docs\demo.ps1
```

The script **pauses after every step**. Press Enter when you're ready for the next one, so you can
explain each call. It prints the request line in yellow and the JSON response below it.

| Step | Call | What to say |
|------|------|-------------|
| 1 | `GET /api/home` | "A guest opens the landing page: bestsellers and new launches, no login needed." |
| 2 | `GET /api/categories` | "Journey step 5: select the category of products." |
| 3 | `GET /api/publishers?categoryId=` | "Journey step 6: browse the brands." |
| 4 | `GET /api/books?categoryId=&publisher=&sort=` | "Filter and sort the catalogue. Each book shows a tentative delivery date." |
| 5 | `GET /api/books?q=` | "Free-text search on title and author." |
| 6 | `GET /api/books/{id}` | "Journey step 7: the product page, with related reads." |
| 7 | `GET /api/cart` (no token) | "A guest cannot use the basket. 401, so login is enforced." |
| 8 | `POST /api/auth/register` | "Register. The password is stored as a BCrypt hash." |
| 9 | `POST /api/auth/login` | "Journey step 1: login. I get a JWT bearer token." |
| 10 | Bad password | "A wrong password returns 401 with a generic message." |
| 11 to 12 | `POST /api/cart/items` | "Journey step 8: add products to the basket. Subtotal, 12% tax and total are calculated." |
| 13 | `PATCH /api/cart/items/{id}` | "Change quantity. Limits and stock are enforced." |
| 14 | `POST /api/addresses` | "Journey step 9: select the delivery address." |
| 15 | Redeem 50 points | "A user can't redeem points they don't have. 400." |
| 16 | `POST /api/orders` | "Journey steps 10 to 12: payment and confirmation. Stock is reduced, the cart is cleared, and gift points are earned." |
| 17 | `GET /api/cart`, `GET /api/me` | "The basket is empty and the points were added." |
| 18 | `GET /api/orders` | "Journey step 2: order history." |
| 19 | `GET /api/orders/buy-again` | "The Buy In Again feature." |
| 20 | `GET /api/home` (logged in) | "Recommendations based on order history: books from the same category that I have not bought." |
| 21 | `POST /api/orders` with points | "A second purchase redeeming 5 gift points, 1 point = Rs 1." |
| 22 | `POST /api/orders/{id}/cancel` | "Cancel within 48 hours: refunded, stock restored, points reversed." |
| 23 | Cancel again | "A second cancel returns 409." |

## Part 3: Show the data in PostgreSQL (about 1 minute)

```powershell
$env:PGPASSWORD = [Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_PASSWORD','User')
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h 127.0.0.1 -U bookstore -d bookstore `
  -c "select id, status, total, payment_status from orders order by id desc limit 5"
Remove-Item Env:PGPASSWORD
```

Say: "The orders are stored in PostgreSQL."

## Part 4: Git and wrap-up (about 1 minute)

1. Show the GitHub repo and the pull request from `feature/api-implementation` into `main`.
2. Show that your manager is a collaborator.
3. Close with a sentence on what AI did well and what you reviewed by hand.

## Tips

- Use a screen recorder such as Windows Game Bar (`Win + G`) or Teams "Record".
- Increase the terminal font size so the JSON is readable.
- Run `Clear-Host` before the demo for a clean screen.
- Don't show `SPRING_DATASOURCE_PASSWORD` or `APP_JWT_SECRET` on screen. Don't run `Get-ChildItem Env:`.
- To run the whole demo without pauses (for a quick check): `powershell -ExecutionPolicy Bypass -File .\docs\demo.ps1 -NoPause`.
