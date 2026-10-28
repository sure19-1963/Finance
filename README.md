# My Finance

A personal finance app for Android, built for the Samsung Galaxy S25 Ultra (it runs on any phone with Android 10 or newer). Everything is stored on the phone and works offline. The default currency is AED, and you can change it in Settings.

## What it does

- **Income and expenses:** record your salary and other income, plus expenses in categories such as credit card, mortgage, loan, home payment plan, tuition, hostel, groceries, rent and more. You can add your own categories.
- **Dashboard:** this month's income, expenses, net savings and savings rate. It also shows spending by category, six months of cash flow, what's due, budget status, and assets against debts. Swipe left or right to change month.
- **Rental properties:** properties, tenants and rental agreements (rent, deposit, number of cheques, due day, end date). You can record rent as it comes in and log maintenance costs. Vacant units and the rent they could be earning are tracked, and you're warned when an agreement is about to end.
- **Budgets:** a monthly limit for each category, with alerts at 80% and when you go over.
- **Debts:** credit cards (limit and usage), loans, mortgages and home payment plans. Each shows interest, payoff progress, an estimated debt-free date, and the interest still to pay. There's an EMI calculator too.
- **Reminders:** notifications before bills, loan EMIs and rent collection are due. Debts and tenants are included automatically.
- **Reports:** monthly and yearly summaries, income and expenses by category, and a month-by-month table. There's also a rental report for each property. Reports can be shared as CSV.
- **Assets and net worth:** savings, investments, gold, vehicles and property values.

## Installing on your phone

Every push to `main` builds the app on GitHub and publishes it as the **latest** release.

1. On your phone, open this repository's **Releases** page and download `MyFinance.apk` from "Latest build".
2. Open the downloaded file. If Android asks, allow installs from your browser or My Files.
3. When the app opens for the first time, allow notifications so reminders can reach you.

Later builds install over the old one and keep your data.

## Building it yourself

You need JDK 17 and the Android SDK (Android Studio installs both).

```
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Tech

Kotlin, Jetpack Compose with Material 3 (it follows your wallpaper colours), Room for the database, and WorkManager for daily reminder checks. The charts are drawn with Compose Canvas.

## Planned

- Backup and restore
- Importing transactions from bank statements
- Sharing data with family members
