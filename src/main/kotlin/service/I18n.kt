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
        "20 пресных простых: 10 мирных и 10 хищных" to "20 freshwater basics: 10 peaceful and 10 predator",
        "50 пресных простых: 25 мирных и 25 хищных" to "50 freshwater basics: 25 peaceful and 25 predator",
        "120 пресных простых: 60 мирных и 60 хищных" to "120 freshwater basics: 60 peaceful and 60 predator",
        "10 морских простых: 3 мирных и 7 хищных" to "10 saltwater basics: 3 peaceful and 7 predator",
        "20 морских простых: 10 мирных и 10 хищных" to "20 saltwater basics: 10 peaceful and 10 predator",
        "20 морских простых: 6 мирных и 14 хищных" to "20 saltwater basics: 6 peaceful and 14 predator",
        "50 морских простых: 15 мирных и 35 хищных" to "50 saltwater basics: 15 peaceful and 35 predator",
        "50 морских простых: 25 мирных и 25 хищных" to "50 saltwater basics: 25 peaceful and 25 predator",
        "120 морских простых: 40 мирных и 80 хищных" to "120 saltwater basics: 40 peaceful and 80 predator",
        "120 морских простых: 60 мирных и 60 хищных" to "120 saltwater basics: 60 peaceful and 60 predator",
        "10 пресных улучшенных: 5 мирных и 5 хищных" to "10 freshwater advanced: 5 peaceful and 5 predator",
        "25 пресных улучшенных: 12 мирных и 13 хищных" to "25 freshwater advanced: 12 peaceful and 13 predator",
        "60 пресных улучшенных: 30 мирных и 30 хищных" to "60 freshwater advanced: 30 peaceful and 30 predator",
        "10 морских улучшенных: 4 мирные+ и 6 хищные+" to "10 saltwater advanced: 4 peaceful+ and 6 predator+",
        "10 морских улучшенных: 5 мирных и 5 хищных" to "10 saltwater advanced: 5 peaceful and 5 predator",
        "25 морских улучшенных: 9 мирных+ и 16 хищных+" to "25 saltwater advanced: 9 peaceful+ and 16 predator+",
        "25 морских улучшенных: 12 мирных и 13 хищных" to "25 saltwater advanced: 12 peaceful and 13 predator",
        "60 морских улучшенных: 20 мирных+ и 40 хищных+" to "60 saltwater advanced: 20 peaceful+ and 40 predator+",
        "60 морских улучшенных: 30 мирных и 30 хищных" to "60 saltwater advanced: 30 peaceful and 30 predator",
        "40 пресных простых (20 мирных и 20 хищных), 20 морских простых (6 мирных и 14 хищных) и 5 пресных улучшенных (3 мирные+ и 2 хищные+)" to
            "40 freshwater basics (20 peaceful and 20 predator), 20 saltwater basics (6 peaceful and 14 predator) and 5 freshwater advanced (3 peaceful+ and 2 predator+)",
        "40 пресных простых (20 мирных и 20 хищных), 20 морских простых (10 мирных и 10 хищных) и 5 пресных улучшенных (3 мирные+ и 2 хищные+)" to
            "40 freshwater basics (20 peaceful and 20 predator), 20 saltwater basics (10 peaceful and 10 predator) and 5 freshwater advanced (3 peaceful+ and 2 predator+)",
        "80 пресных простых (40 мирных и 40 хищных), 40 морских простых (12 мирных и 28 хищных), 15 пресных улучшенных (8 мирных+ и 7 хищных+) и 5 морских улучшенных (1 мирная+ и 4 хищные+)" to
            "80 freshwater basics (40 peaceful and 40 predator), 40 saltwater basics (12 peaceful and 28 predator), 15 freshwater advanced (8 peaceful+ and 7 predator+) and 5 saltwater advanced (1 peaceful+ and 4 predator+)",
        "80 пресных простых (40 мирных и 40 хищных), 40 морских простых (20 мирных и 20 хищных), 15 пресных улучшенных (8 мирных+ и 7 хищных+) и 5 морских улучшенных (3 мирные+ и 2 хищные+)" to
            "80 freshwater basics (40 peaceful and 40 predator), 40 saltwater basics (20 peaceful and 20 predator), 15 freshwater advanced (8 peaceful+ and 7 predator+) and 5 saltwater advanced (3 peaceful+ and 2 predator+)",
        "200 пресных простых (100 мирных и 100 хищных), 120 морских простых (40 мирных и 80 хищных), 40 пресных улучшенных (20 мирных+ и 20 хищных+) и 20 морских улучшенных (6 мирных+ и 14 хищных+)" to
            "200 freshwater basics (100 peaceful and 100 predator), 120 saltwater basics (40 peaceful and 80 predator), 40 freshwater advanced (20 peaceful+ and 20 predator+) and 20 saltwater advanced (6 peaceful+ and 14 predator+)",
        "200 пресных простых (100 мирных и 100 хищных), 120 морских простых (60 мирных и 60 хищных), 40 пресных улучшенных (20 мирных+ и 20 хищных+) и 20 морских улучшенных (10 мирных+ и 10 хищных+)" to
            "200 freshwater basics (100 peaceful and 100 predator), 120 saltwater basics (60 peaceful and 60 predator), 40 freshwater advanced (20 peaceful+ and 20 predator+) and 20 saltwater advanced (10 peaceful+ and 10 predator+)",
        "15 пресных хищных" to "15 freshwater predator",
        "20 морских хищных" to "20 saltwater predator",
        "10 морских простых: 5 мирных и 5 хищных" to "10 saltwater basics: 5 peaceful and 5 predator",
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
        "Пресная мирная" -> "Freshwater Peaceful"
        "Пресная хищная" -> "Freshwater Predator"
        "Морская мирная" -> "Saltwater Peaceful"
        "Морская хищная" -> "Saltwater Predator"
        "Пресная мирная+" -> "Freshwater Peaceful+"
        "Пресная хищная+" -> "Freshwater Predator+"
        "Морская мирная+" -> "Saltwater Peaceful+"
        "Морская хищная+" -> "Saltwater Predator+"
        else -> name
    }

    fun lureDescription(name: String, lang: String): String {
        val texts = lures[name] ?: return ""
        return if (lang == "ru") texts.ruDescription else texts.enDescription
    }
    fun rod(name: String, lang: String) = if (lang == "en") rods[name] ?: name else name
    fun text(str: String, lang: String) = if (lang == "en") texts[str] ?: str else str
}
