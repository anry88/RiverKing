package service

object I18n {
    private val fish = mapOf(
        "Плотва" to "Roach",
        "Окунь" to "Perch",
        "Карась" to "Crucian Carp",
        "Лещ" to "Bream",
        "Щука" to "Pike",
        "Карп" to "Carp",
        "Сом" to "Catfish",
        "Осётр" to "Sturgeon",
        "Уклейка" to "Bleak",
        "Линь" to "Tench",
        "Ротан" to "Rotan",
        "Судак" to "Zander",
        "Чехонь" to "Sabrefish",
        "Хариус" to "Grayling",
        "Форель ручьевая" to "Brook Trout",
        "Таймень" to "Taimen",
        "Налим" to "Burbot",
        "Сиг" to "Whitefish",
        "Голавль" to "Chub",
        "Жерех" to "Asp",
        "Толстолобик" to "Bighead Carp",
        "Белый амур" to "Grass Carp",
        "Угорь европейский" to "European Eel",
        "Стерлядь" to "Sterlet",
        "Кефаль" to "Mullet",
        "Камбала" to "Flounder",
        "Сельдь" to "Herring",
        "Ставрида" to "Horse Mackerel",
        "Треска" to "Cod",
        "Сайда" to "Pollock",
        "Морская форель" to "Sea Trout",
        "Палтус" to "Halibut",
        "Корюшка" to "Smelt",
        "Лосось атлантический" to "Atlantic Salmon",
        "Лаврак" to "Sea Bass",
        "Скумбрия атлантическая" to "Atlantic Mackerel",
        "Белуга" to "Beluga",
        "Ёрш" to "Ruffe",
        "Пескарь" to "Gudgeon",
        "Густера" to "Blue Bream",
        "Краснопёрка" to "Rudd",
        "Елец" to "Dace",
        "Верхоплавка" to "Topmouth Gudgeon",
        "Гольян" to "Minnow",
        "Язь" to "Ide",
        "Бычок" to "Goby",
        "Килька" to "Sprat",
        "Мойва" to "Capelin",
        "Сардина" to "Sardine",
        "Анчоус" to "Anchovy",
        "Дорадо" to "Dorado",
        "Ваху" to "Wahoo",
        "Парусник" to "Sailfish",
        "Рыба-меч" to "Swordfish",
        "Марлин синий" to "Blue Marlin",
        "Тунец синеперый" to "Bluefin Tuna",
        "Акула мако" to "Mako Shark",
        "Альбакор" to "Albacore",
        "Голец арктический" to "Arctic Char",
        "Форель кумжа" to "Brown Trout",
        "Пикша" to "Haddock",
        "Тюрбо" to "Turbot",
        "Сайра" to "Pacific Saury",
        "Летучая рыба" to "Flying Fish",
        "Рыба-луна" to "Ocean Sunfish",
        "Сельдяной король" to "Oarfish",
        "Тамбаки" to "Tambaqui",
        "Паку чёрный" to "Black Pacu",
        "Прохилодус" to "Prochilodus",
        "Анциструс" to "Ancistrus",
        "Отоцинклюс" to "Otocinclus",
        "Неоновая тетра" to "Neon Tetra",
        "Тернеция" to "Black Tetra",
        "Арапайма" to "Arapaima",
        "Пиранья краснобрюхая" to "Red-bellied Piranha",
        "Трайра" to "Trahira",
        "Ацестринх" to "Bicuda",
        "Электрический угорь" to "Electric Eel",
        "Краснохвостый сом" to "Redtail Catfish",
        "Пираиба" to "Piraiba",
        "Дискус" to "Discus",
        "Скалярия" to "Angelfish",
        "Апистограмма Агассиза" to "Agassiz's Cichlid",
        "Кардинальная тетра" to "Cardinal Tetra",
        "Коридорас панда" to "Panda Cory",
        "Нанностомус" to "Pencilfish",
        "Рамирези" to "Ram Cichlid",
        "Аравана чёрная" to "Black Arowana",
        "Оскар" to "Oscar",
        "Аймара" to "Aimara",
        "Псевдоплатистома тигровая" to "Tiger Shovelnose",
        "Пиранья чёрная" to "Black Piranha",
        "Щучья цихлида" to "Pike Cichlid",
        "Павлиний окунь" to "Peacock Bass",
        "Молочная рыба" to "Milkfish",
        "Пятнистая кефаль" to "Spotted Mullet",
        "Тиляпия мозамбикская" to "Mozambique Tilapia",
        "Анчоус тропический" to "Tropical Anchovy",
        "Сардина индийская" to "Indian Sardine",
        "Золотистый сиган" to "Golden Rabbitfish",
        "Бычок мангровый" to "Mangrove Goby",
        "Баррамунди" to "Barramundi",
        "Снук" to "Snook",
        "Мангровый луциан" to "Mangrove Snapper",
        "Тарпон" to "Tarpon",
        "Морской сом" to "Sea Catfish",
        "Морской сарган" to "Needlefish",
        "Голубой каранкс" to "Blue Trevally",
        "Рыба-попугай" to "Parrotfish",
        "Императорский ангел" to "Emperor Angelfish",
        "Голубой хирург" to "Blue Tang",
        "Нитеносная бабочка" to "Threadfin Butterflyfish",
        "Синяя хризиптера" to "Blue Damselfish",
        "Фузилёр жёлтохвостый" to "Yellowtail Fusilier",
        "Барабулька тропическая" to "Tropical Goatfish",
        "Большая барракуда" to "Great Barracuda",
        "Гигантский каранкс" to "Giant Trevally",
        "Пермит" to "Permit",
        "Альбула" to "Bonefish",
        "Испанская скумбрия" to "Spanish Mackerel",
        "Коралловая форель" to "Coral Trout",
        "Спинорог-титан" to "Titan Triggerfish",
        "Кои (Кохаку)" to "Koi (Kohaku)",
        "Змееголов северный" to "Northern Snakehead",
        "Амурская щука" to "Amur Pike",
        "Кристивомер" to "Lake Trout (Cristivomer)",
        "Дунайский лосось" to "Danube Salmon (Huchen)",
        "Чёрный амур" to "Black Amur",
        "Пеленгас" to "Pelingas Mullet",
        "Кубера" to "Cubera Snapper",
        "Зубатка пятнистая" to "Spotted Wolffish",
        "Тунец желтоперый" to "Yellowfin Tuna",
        "Зунгаро" to "Zungaro Catfish",
        "Скат моторо" to "Motoro Stingray",
        "Чёрный снук" to "Black Snook",
        "Рыба-наполеон" to "Napoleon Wrasse",
    )

    private val locations = mapOf(
        "Пруд" to "Pond",
        "Река" to "River",
        "Озеро" to "Lake",
        "Болото" to "Swamp",
        "Горная река" to "Mountain River",
        "Водохранилище" to "Reservoir",
        "Дельта реки" to "River Delta",
        "Прибрежье моря" to "Sea Coast",
        "Фьорд" to "Fjord",
        "Открытый океан" to "Open Ocean",
        "Русло Амазонки" to "Amazon Riverbed",
        "Игапо, затопленный лес" to "Igapo Flooded Forest",
        "Мангровые заросли" to "Mangroves",
        "Коралловые отмели" to "Coral Flats",
    )

    private data class LureTexts(
        val ruName: String,
        val enName: String,
        val ruDescription: String,
        val enDescription: String,
    )

    private val lures = mapOf(
        "Пресная мирная" to LureTexts(
            ruName = "Зерновая крошка",
            enName = "Grain Crumble",
            ruDescription = "Для мирной пресноводной рыбы.",
            enDescription = "For peaceful freshwater fish.",
        ),
        "Пресная хищная" to LureTexts(
            ruName = "Ручейный малек",
            enName = "Brook Minnow",
            ruDescription = "Для хищной пресноводной рыбы.",
            enDescription = "For predatory freshwater fish.",
        ),
        "Морская мирная" to LureTexts(
            ruName = "Морская водоросль",
            enName = "Seaweed Strand",
            ruDescription = "Для мирной морской рыбы.",
            enDescription = "For peaceful saltwater fish.",
        ),
        "Морская хищная" to LureTexts(
            ruName = "Кольца кальмара",
            enName = "Squid Rings",
            ruDescription = "Для хищной морской рыбы.",
            enDescription = "For predatory saltwater fish.",
        ),
        "Пресная мирная+" to LureTexts(
            ruName = "Луговой червь",
            enName = "Meadow Worm",
            ruDescription = "Для редкой мирной пресноводной рыбы.",
            enDescription = "For rare peaceful freshwater fish.",
        ),
        "Пресная хищная+" to LureTexts(
            ruName = "Серебряный живец",
            enName = "Silver Shiner",
            ruDescription = "Для редкой хищной пресноводной рыбы.",
            enDescription = "For rare predatory freshwater fish.",
        ),
        "Морская мирная+" to LureTexts(
            ruName = "Неоновый планктон",
            enName = "Neon Plankton",
            ruDescription = "Для редкой мирной морской рыбы.",
            enDescription = "For rare peaceful saltwater fish.",
        ),
        "Морская хищная+" to LureTexts(
            ruName = "Королевская креветка",
            enName = "Royal Shrimp",
            ruDescription = "Для редкой хищной морской рыбы.",
            enDescription = "For rare predatory saltwater fish.",
        ),
    )

    private val rods = mapOf(
        "Искра" to "Spark",
        "Роса" to "Dew",
        "Поток" to "Stream",
        "Глубь" to "Abyss",
        "Шторм" to "Storm",
    )

    private val texts = mapOf(
        "Пресные простые" to "Freshwater Basics",
        "Морские простые" to "Saltwater Basics",
        "Пресные улучшенные" to "Freshwater Advanced",
        "Морские улучшенные" to "Saltwater Advanced",
        "Смешанные" to "Mixed",
        "Стартовые" to "Starter",
        "Пополнение S" to "Top-up S",
        "Запас M" to "Stock M",
        "Ящик L" to "Crate L",
        "Буст S" to "Boost S",
        "Буст M" to "Boost M",
        "Буст L" to "Boost L",
        "Стартовый набор" to "Starter Pack",
        "Профи рыболов" to "Pro Angler",
        "Китовый ящик" to "Whale Crate",
        "Пополнение пресных хищных" to "Predator Top-up",
        "Морской старт" to "Sea Start",
        "Морской хищный запас" to "Saltwater Predator Stock",
        "20 пресных простых: 10 «Зерновая крошка» и 10 «Ручейный малек»" to "20 freshwater basics: 10 \"Grain Crumble\" and 10 \"Brook Minnow\"",
        "50 пресных простых: 25 «Зерновая крошка» и 25 «Ручейный малек»" to "50 freshwater basics: 25 \"Grain Crumble\" and 25 \"Brook Minnow\"",
        "120 пресных простых: 60 «Зерновая крошка» и 60 «Ручейный малек»" to "120 freshwater basics: 60 \"Grain Crumble\" and 60 \"Brook Minnow\"",
        "10 морских простых: 3 «Морская водоросль» и 7 «Кольца кальмара»" to "10 saltwater basics: 3 \"Seaweed Strand\" and 7 \"Squid Rings\"",
        "20 морских простых: 10 «Морская водоросль» и 10 «Кольца кальмара»" to "20 saltwater basics: 10 \"Seaweed Strand\" and 10 \"Squid Rings\"",
        "20 морских простых: 6 «Морская водоросль» и 14 «Кольца кальмара»" to "20 saltwater basics: 6 \"Seaweed Strand\" and 14 \"Squid Rings\"",
        "50 морских простых: 15 «Морская водоросль» и 35 «Кольца кальмара»" to "50 saltwater basics: 15 \"Seaweed Strand\" and 35 \"Squid Rings\"",
        "50 морских простых: 25 «Морская водоросль» и 25 «Кольца кальмара»" to "50 saltwater basics: 25 \"Seaweed Strand\" and 25 \"Squid Rings\"",
        "120 морских простых: 40 «Морская водоросль» и 80 «Кольца кальмара»" to "120 saltwater basics: 40 \"Seaweed Strand\" and 80 \"Squid Rings\"",
        "120 морских простых: 60 «Морская водоросль» и 60 «Кольца кальмара»" to "120 saltwater basics: 60 \"Seaweed Strand\" and 60 \"Squid Rings\"",
        "10 пресных улучшенных: 5 «Луговой червь» и 5 «Серебряный живец»" to "10 freshwater advanced: 5 \"Meadow Worm\" and 5 \"Silver Shiner\"",
        "25 пресных улучшенных: 12 «Луговой червь» и 13 «Серебряный живец»" to "25 freshwater advanced: 12 \"Meadow Worm\" and 13 \"Silver Shiner\"",
        "60 пресных улучшенных: 30 «Луговой червь» и 30 «Серебряный живец»" to "60 freshwater advanced: 30 \"Meadow Worm\" and 30 \"Silver Shiner\"",
        "10 морских улучшенных: 4 «Неоновый планктон» и 6 «Королевская креветка»" to "10 saltwater advanced: 4 \"Neon Plankton\" and 6 \"Royal Shrimp\"",
        "10 морских улучшенных: 5 «Неоновый планктон» и 5 «Королевская креветка»" to "10 saltwater advanced: 5 \"Neon Plankton\" and 5 \"Royal Shrimp\"",
        "25 морских улучшенных: 9 «Неоновый планктон» и 16 «Королевская креветка»" to "25 saltwater advanced: 9 \"Neon Plankton\" and 16 \"Royal Shrimp\"",
        "25 морских улучшенных: 12 «Неоновый планктон» и 13 «Королевская креветка»" to "25 saltwater advanced: 12 \"Neon Plankton\" and 13 \"Royal Shrimp\"",
        "60 морских улучшенных: 20 «Неоновый планктон» и 40 «Королевская креветка»" to "60 saltwater advanced: 20 \"Neon Plankton\" and 40 \"Royal Shrimp\"",
        "60 морских улучшенных: 30 «Неоновый планктон» и 30 «Королевская креветка»" to "60 saltwater advanced: 30 \"Neon Plankton\" and 30 \"Royal Shrimp\"",
        "40 пресных простых (20 «Зерновая крошка» и 20 «Ручейный малек»), 20 морских простых (6 «Морская водоросль» и 14 «Кольца кальмара») и 5 пресных улучшенных (3 «Луговой червь» и 2 «Серебряный живец»)" to
            "40 freshwater basics (20 \"Grain Crumble\" and 20 \"Brook Minnow\"), 20 saltwater basics (6 \"Seaweed Strand\" and 14 \"Squid Rings\") and 5 freshwater advanced (3 \"Meadow Worm\" and 2 \"Silver Shiner\")",
        "40 пресных простых (20 «Зерновая крошка» и 20 «Ручейный малек»), 20 морских простых (10 «Морская водоросль» и 10 «Кольца кальмара») и 5 пресных улучшенных (3 «Луговой червь» и 2 «Серебряный живец»)" to
            "40 freshwater basics (20 \"Grain Crumble\" and 20 \"Brook Minnow\"), 20 saltwater basics (10 \"Seaweed Strand\" and 10 \"Squid Rings\") and 5 freshwater advanced (3 \"Meadow Worm\" and 2 \"Silver Shiner\")",
        "80 пресных простых (40 «Зерновая крошка» и 40 «Ручейный малек»), 40 морских простых (12 «Морская водоросль» и 28 «Кольца кальмара»), 15 пресных улучшенных (8 «Луговой червь» и 7 «Серебряный живец») и 5 морских улучшенных (1 «Неоновый планктон» и 4 «Королевская креветка»)" to
            "80 freshwater basics (40 \"Grain Crumble\" and 40 \"Brook Minnow\"), 40 saltwater basics (12 \"Seaweed Strand\" and 28 \"Squid Rings\"), 15 freshwater advanced (8 \"Meadow Worm\" and 7 \"Silver Shiner\") and 5 saltwater advanced (1 \"Neon Plankton\" and 4 \"Royal Shrimp\")",
        "80 пресных простых (40 «Зерновая крошка» и 40 «Ручейный малек»), 40 морских простых (20 «Морская водоросль» и 20 «Кольца кальмара»), 15 пресных улучшенных (8 «Луговой червь» и 7 «Серебряный живец») и 5 морских улучшенных (3 «Неоновый планктон» и 2 «Королевская креветка»)" to
            "80 freshwater basics (40 \"Grain Crumble\" and 40 \"Brook Minnow\"), 40 saltwater basics (20 \"Seaweed Strand\" and 20 \"Squid Rings\"), 15 freshwater advanced (8 \"Meadow Worm\" and 7 \"Silver Shiner\") and 5 saltwater advanced (3 \"Neon Plankton\" and 2 \"Royal Shrimp\")",
        "200 пресных простых (100 «Зерновая крошка» и 100 «Ручейный малек»), 120 морских простых (40 «Морская водоросль» и 80 «Кольца кальмара»), 40 пресных улучшенных (20 «Луговой червь» и 20 «Серебряный живец») и 20 морских улучшенных (6 «Неоновый планктон» и 14 «Королевская креветка»)" to
            "200 freshwater basics (100 \"Grain Crumble\" and 100 \"Brook Minnow\"), 120 saltwater basics (40 \"Seaweed Strand\" and 80 \"Squid Rings\"), 40 freshwater advanced (20 \"Meadow Worm\" and 20 \"Silver Shiner\") and 20 saltwater advanced (6 \"Neon Plankton\" and 14 \"Royal Shrimp\")",
        "200 пресных простых (100 «Зерновая крошка» и 100 «Ручейный малек»), 120 морских простых (60 «Морская водоросль» и 60 «Кольца кальмара»), 40 пресных улучшенных (20 «Луговой червь» и 20 «Серебряный живец») и 20 морских улучшенных (10 «Неоновый планктон» и 10 «Королевская креветка»)" to
            "200 freshwater basics (100 \"Grain Crumble\" and 100 \"Brook Minnow\"), 120 saltwater basics (60 \"Seaweed Strand\" and 60 \"Squid Rings\"), 40 freshwater advanced (20 \"Meadow Worm\" and 20 \"Silver Shiner\") and 20 saltwater advanced (10 \"Neon Plankton\" and 10 \"Royal Shrimp\")",
        "15 «Ручейный малек»" to "15 \"Brook Minnow\"",
        "20 «Кольца кальмара»" to "20 \"Squid Rings\"",
        "10 морских простых: 5 «Морская водоросль» и 5 «Кольца кальмара»" to "10 saltwater basics: 5 \"Seaweed Strand\" and 5 \"Squid Rings\"",
        "Подписки" to "Subscriptions",
        "Автоловля" to "Auto Catch",
        "Робот ловит за вас целый месяц и не упустит ни одной рыбы" to "Robot catches fish for you for a whole month and won't miss any fish",
    )

    fun fish(name: String, lang: String) = if (lang == "en") fish[name] ?: name else name
    fun location(name: String, lang: String) = if (lang == "en") locations[name] ?: name else name
    fun lure(name: String, lang: String): String {
        val texts = lures[name]
        return when {
            texts == null -> if (lang == "en") oldLureName(name) else name
            lang == "ru" -> texts.ruName
            else -> texts.enName
        }
    }

    private fun oldLureName(name: String) = when (name) {
        "Пресная мирная" -> "Grain Crumble"
        "Пресная хищная" -> "Brook Minnow"
        "Морская мирная" -> "Seaweed Strand"
        "Морская хищная" -> "Squid Rings"
        "Пресная мирная+" -> "Meadow Worm"
        "Пресная хищная+" -> "Silver Shiner"
        "Морская мирная+" -> "Neon Plankton"
        "Морская хищная+" -> "Royal Shrimp"
        else -> name
    }

    fun lureDescription(name: String, lang: String): String {
        val texts = lures[name] ?: return ""
        return if (lang == "ru") texts.ruDescription else texts.enDescription
    }
    fun rod(name: String, lang: String) = if (lang == "en") rods[name] ?: name else name
    fun text(str: String, lang: String) = if (lang == "en") texts[str] ?: str else str
}
