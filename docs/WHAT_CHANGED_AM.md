# Ինչ է փոխվել․ հայերեն ամփոփում

Նախագծի սկզբնական տարբերակը մշակվել է **Lilit Martirosyan-ի և Mariam Vardanyan-ի խմբային աշխատանքի** շրջանակում։ GitHub-ի համար այս թղթապանակը պատրաստվել է սկզբնական SQL, Java և Spring Boot նյութերի հիման վրա՝ AI-ի օգնությամբ վերանայման ու բարելավման արդյունքում։ Հեղինակային ներդրումները առանձին անդամների միջև դեռ չեն բաժանվել, քանի որ ներկայացված նյութերից դա հնարավոր չէ վստահ որոշել։

## Իրականացված բարելավումներ

1. **Միասնական repository** — backend-ը, database-ը, examples-ը, docs-ը և ստուգման scripts-ը հավաքվել են մի տեղում։
2. **Անվտանգ configuration** — հեռացվել են սկզբնական կոդում եղած database credentials-ը և SQL login passwords-ը։ Դրանք հիմա մուտքագրվում են environment variables-ով։
3. **Demo API key** — ավելացվել է shared API key-ով մուտքի սահմանափակում։ **Սա դեռ լիարժեք օգտատերերի authentication կամ authorization չէ։**
4. **Rule correctness** — բազմապայման կանոնների ստուգումն ու preview-ը օգտագործում են միևնույն տրամաբանությունը։ Preview-ը չի փոխում սենսորների պահված վիճակը։
5. **Sensor identity** — նոր կանոնների պայմաններում պահվում է ճշգրիտ `sensor_id`-ը։ Հին կոդի սխալը (բոլոր sensor-ները `motion` դարձնելը) վերացվել է։
6. **Device state** — `is_active` (հասանելիություն) և `on_state` (միացված/անջատված) իմաստները բաժանվել են։
7. **Event և execution logs** — event-ը պահպանվում է մեկ անգամ, իսկ simulated device-state փոփոխությունն ու դրա execution log-ը կատարվում են մեկ database transaction-ով։
8. **Data structures** — շտկվել է queue-ի չափի հաշվարկը, ավելացվել է cycle-ի մերժում և graph-ից կրկնվող գործողությունների կանխարգելում։
9. **API** — տվյալներ փոփոխող GET endpoint-ները փոխվել են POST-ի, ավելացվել է մուտքային տվյալների հիմնական ստուգում և error handling։
10. **SQL** — ստեղծվել է վերարտադրելի schema, demo seed, indexes/views/triggers/procedures և հին database-ի migration script։
11. **Tests և documentation** — ավելացվել են engine smoke test, 7 JUnit unit tests, CI, README, installation guide, API և architecture նկարագրություններ։

## Արդյունքի ստուգում

- **Հաջողվել է**՝ Java engine-ի իրական `javac` compile, անկախ smoke test, ամբողջ Java source-ի compile/syntax ստուգում ժամանակավոր framework stubs-ով, XML validation և repository-ի ֆայլերի/security preflight ստուգում։
- **Դեռ չի ստուգվել**՝ ամբողջ Spring Boot application-ի Maven build-ը, SQL Server-ի վրա սցենարների գործարկումը, իրական REST integration-ը։ Դրանք պարտադիր են վերջնական հրապարակումից առաջ։

## Հրապարակումից առաջ

Երկու թիմակիցներն էլ պետք է համաձայնեն credits-ի, repository-ի owner-ի և license-ի շուրջ։ Հին PDF-ը **չի ներառվել**, որովհետև պարունակում է հին fixed passwords և մասամբ արդեն հնացած SQL հատվածներ։ Անհրաժեշտ է փոխել նախկինում օգտագործված գաղտնաբառերը, կատարել live integration test և հետո միայն հրապարակել GitHub-ում։
