It took roughly 6-7 hours of work 

## Thought Process
1) 1) I first wanted to understand the code and wanted to add the methods that will help in the payment process: fetch
      invoices by status and update the paid invoices to PAID.
   2) I also added some tests to check the existing methods and the new ones.
   3) I updated the version of sqlite since the older version doesn't work on m chips.
2) Added the payment timestamp to the invoice table to know when the invoice was paid, which will always be useful
3) Updated the fetch invoice endpoint to filter it by status, which enables us to check more easily which invoices were
   already paid and which are still pending
4) Added the bulk update invoices to paid to the Invoice service
5) 1) Normally this would be an event triggered by AWS CloudWatch cron job for example. Nonetheless, I used the timer
      class to schedule the payPendingInvoices function to run on the next month in the first day at midnight.
   2) Using the first scheduleAtFixedRate I calculate every day how much time there is left until the next first day of
         the month. If this calculation is less or equal than a day (I do this to avoid the creation of multiple calls to
         be triggered on the 1st of the next month), a schedule is being used to run the payPendingInvoices just once when
         the first day of the month is reached.
   3) The payPendingInvoices function tries to pay every pending invoice. If a payment fails because of a Network failure
      or an unknown reason we can set maximumNumberOfTries to a number bigger than one, and it will try again after 1
      second. I did this because the reason for failure might be fixed after some time. This function also returns the
      ids of the paid invoices and the ids of failed payment invoices and the reason why it failed

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
