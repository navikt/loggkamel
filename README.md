# Loggkamel

Loggkamel overfører logger fra on-prem databaser til GCP.

Generert med https://kameleon.dev

Dokumentasjon:
https://confluence.adeo.no/spaces/TM/pages/760453055/Loggtransport

Slackkanal:
#team-sikkerhetstjenesten

## Usage (for other teams)

TODO: expectations re: input file format, entry location for pushed logs, etc

## Program design and intent

TODO: diagram of arkiv flow

TODO: high level description of intended flow

## Arkiv Controller

TODO: description of what the controller is intended for, how it is intended to be used, etc

### Swagger

DEV: https://loggkamel.intern.dev.nav.no/swagger-ui/index.html#/

PROD: https://loggkamel.intern.nav.no/swagger-ui/index.html#/

## Kjore lokalt med PostgreSQL

Applikasjonen er satt opp til a bruke PostgreSQL i `local`-profilen (ikke H2).

### Alternativ 1: Lokal proxy mot dev DB (anbefalt)

Start your local proxy with:

```zsh
nais postgres proxy --team sikkerhetstjenesten --environment dev-gcp --reason "debugging issue" loggkamel
```

Standard lokal JDBC-url er:

- `jdbc:postgresql://localhost:5432/loggkamel`

Brukernavn settes via miljo-variabel (ikke hardkodet i URL):

```zsh
export LOCAL_DB_USERNAME="$(whoami)@nav.no"
export LOCAL_DB_PASSWORD=""
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Hvis du trenger a overstyre URL:

```zsh
export LOCAL_DB_JDBC_URL="jdbc:postgresql://localhost:5432/loggkamel"
```

### Alternativ 2: Egen lokal PostgreSQL

Hvis du ikke bruker proxy, kan du starte en lokal Postgres-container:

```zsh
docker run --name loggkamel-postgres \
  -e POSTGRES_DB=loggkamel \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16
```

Kjor appen mot den lokale databasen:

```zsh
export LOCAL_DB_JDBC_URL="jdbc:postgresql://localhost:5432/loggkamel"
export LOCAL_DB_USERNAME="postgres"
export LOCAL_DB_PASSWORD="postgres"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

