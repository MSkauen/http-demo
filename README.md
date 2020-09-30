![Java CI with Maven](https://github.com/kristiania/pgr203innlevering2-mathiasharestad-martinsorhus/workflows/Java%20CI%20with%20Maven/badge.svg)

# pgr203innlevering2-mathiasharestad-martinsorhus
pgr203innlevering2-martinsorhus created by GitHub Classroom

Denne oppgaven gir en mulighet til å starte opp en HTTP-server og vil kunne levere statiske filer slik som .html og .css filer.

1.   For å komme igang må du først klone prosjektet, deretter bygge serveren med kommandoen `mvn package`

        Dersom du får feilmelding _'mvn is not recognized as an internal or external command'_ er du nødt til å sette riktig PATH for maven i miljø variabler.

1.   Ellers burde maven bygge serveren uten problemer og du vil kunne se hvor JAR filen er blitt plassert i terminalen.
        Standard er dette 'target\' mappen til prosjektet.

1.   For å kjøre serveren bruker du kommandoen `java -jar target\pgr203innlevering2-mathiasharestad-martinsorhus-1.0-SNAPSHOT-shaded.jar`

Om serveren startet opp uten problemer vil du bli gitt informasjon i terminalen om hvilken port serveren er startet på samt hvilken URL du bruker for å få tilgang til den.