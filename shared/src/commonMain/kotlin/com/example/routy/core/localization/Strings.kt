package com.example.routy.core.localization

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.routy.core.transport.domain.*

enum class TextKey {
    AppName,
    City,
    Map,
    Routes,
    Stops,
    Favorites,
    Settings,
    Search,
    SearchRoutes,
    SearchStops,
    Explore,
    ExploreBody,
    AllRoutes,
    Nearby,
    Retry,
    Loading,
    Empty,
    NoResults,
    NoFavorites,
    Offline,
    Refreshing,
    NoInternet,
    Timeout,
    Server,
    Invalid,
    Storage,
    Unknown,
    Schedule,
    ScheduleNote,
    NextDeparture,
    Departure,
    NoSchedule,
    Group,
    Unspecified,
    Save,
    Saved,
    Remove,
    RemoveFavoriteTitle,
    RemoveFavoriteBody,
    ShowMap,
    Back,
    Close,
    Live,
    Stale,
    NoBuses,
    MyLocation,
    LocationHelp,
    LocationUnavailable,
    Language,
    Appearance,
    ColorTheme,
    Ocean,
    Mint,
    Mono,
    System,
    Light,
    Dark,
    English,
    Georgian,
    Russian,
    About,
    MapUnavailable,
    FitRoute,
    Vehicle,
    Details,
    QuickSelect,
    SearchEmptyTitle,
    SearchEmptyBody,
}

class Strings(
    val language: Language,
) {
    operator fun get(key: TextKey): String =
        when (language) {
            Language.English -> english
            Language.Georgian -> georgian
            Language.Russian -> russian
        }.getValue(key)

    fun error(error: AppError): String =
        get(
            when (error) {
                AppError.NoInternet -> TextKey.NoInternet
                AppError.Timeout -> TextKey.Timeout
                AppError.ServerUnavailable -> TextKey.Server
                AppError.InvalidData -> TextKey.Invalid
                AppError.StorageUnavailable -> TextKey.Storage
                AppError.Unknown -> TextKey.Unknown
            },
        )

    fun stopsCount(count: Int): String =
        when (language) {
            Language.English -> "$count ${if (count == 1) "stop" else "stops"}"
            Language.Georgian -> "$count გაჩერება"
            Language.Russian -> "$count ${russianStops(count)}"
        }

    fun vehiclesCount(count: Int): String =
        when (language) {
            Language.English -> "$count ${if (count == 1) "bus" else "buses"}"
            Language.Georgian -> "$count ავტობუსი"
            Language.Russian -> "$count ${russianBuses(count)}"
        }

    fun runsEvery(minutes: Int): String =
        when (language) {
            Language.English -> "Every $minutes min"
            Language.Georgian -> "ყოველ $minutes წუთში"
            Language.Russian -> "Каждые $minutes мин."
        }

    fun minutesShort(minutes: Int): String =
        when (language) {
            Language.English -> "$minutes min"
            Language.Georgian -> "$minutes წთ"
            Language.Russian -> "$minutes мин"
        }
}

private fun russianStops(count: Int): String =
    when (val remainder = count % 100) {
        in 11..14 -> "остановок"
        else ->
            when (remainder % 10) {
                1 -> "остановка"
                in 2..4 -> "остановки"
                else -> "остановок"
            }
    }

private fun russianBuses(count: Int): String =
    when (val remainder = count % 100) {
        in 11..14 -> "автобусов"
        else ->
            when (remainder % 10) {
                1 -> "автобус"
                in 2..4 -> "автобуса"
                else -> "автобусов"
            }
    }

val LocalStrings = staticCompositionLocalOf { Strings(Language.English) }
private val english =
    mapOf(
        TextKey.AppName to "Routy",
        TextKey.City to "BATUMI • PUBLIC TRANSPORT",
        TextKey.Map to "Map",
        TextKey.Routes to "Routes",
        TextKey.Stops to "Stops",
        TextKey.Favorites to "Saved",
        TextKey.Settings to "Settings",
        TextKey.Search to "Find a route or stop",
        TextKey.SearchRoutes to "Route number or name",
        TextKey.SearchStops to "Stop name or number",
        TextKey.Explore to "Your city, connected",
        TextKey.ExploreBody to "Find your next ride through Batumi",
        TextKey.AllRoutes to "All routes",
        TextKey.Nearby to "Explore stops",
        TextKey.Retry to "Try again",
        TextKey.Loading to "Loading transport data…",
        TextKey.Empty to "Nothing here yet",
        TextKey.NoResults to "No matches. Try another name or number.",
        TextKey.NoFavorites to "Save routes and stops to find them here.",
        TextKey.Offline to "Saved data • updates unavailable",
        TextKey.Refreshing to "Updating transport data…",
        TextKey.NoInternet to "No connection. Check your internet and retry.",
        TextKey.Timeout to "The connection took too long. Try again.",
        TextKey.Server to "Transport service is unavailable. Try again later.",
        TextKey.Invalid to "Transport data could not be read. Try refreshing.",
        TextKey.Storage to "Changes could not be saved on this device.",
        TextKey.Unknown to "Something went wrong. Please try again.",
        TextKey.Schedule to "Scheduled departures",
        TextKey.ScheduleNote to "Batumi time • timetable, not live arrival predictions",
        TextKey.NextDeparture to "Next departure",
        TextKey.Departure to "Departure",
        TextKey.NoSchedule to "No timetable available",
        TextKey.Group to "Stop group",
        TextKey.Unspecified to "Unspecified group",
        TextKey.Save to "Save",
        TextKey.Saved to "Saved",
        TextKey.Remove to "Remove",
        TextKey.RemoveFavoriteTitle to "Remove from saved?",
        TextKey.RemoveFavoriteBody to "You can add it again later.",
        TextKey.ShowMap to "Show on map",
        TextKey.Back to "Back",
        TextKey.Close to "Close",
        TextKey.Live to "Live buses",
        TextKey.Stale to "Last known positions • updates unavailable",
        TextKey.NoBuses to "No buses reported on this route",
        TextKey.MyLocation to "My location",
        TextKey.LocationHelp to "Location is optional and used only to show you on the map.",
        TextKey.LocationUnavailable to "Location unavailable. Check device permissions.",
        TextKey.Language to "Language",
        TextKey.Appearance to "Appearance",
        TextKey.ColorTheme to "Color theme",
        TextKey.Ocean to "Ocean",
        TextKey.Mint to "Mint",
        TextKey.Mono to "Mono",
        TextKey.System to "Use device setting",
        TextKey.Light to "Light",
        TextKey.Dark to "Dark",
        TextKey.English to "English",
        TextKey.Georgian to "ქართული",
        TextKey.Russian to "Русский",
        TextKey.About to "Made for everyday journeys in Batumi",
        TextKey.MapUnavailable to "Map tiles unavailable. Routes and stops remain accessible from the lists.",
        TextKey.FitRoute to "Fit route",
        TextKey.Vehicle to "Bus",
        TextKey.Details to "Details",
        TextKey.QuickSelect to "Quick pick",
        TextKey.SearchEmptyTitle to "Nothing found",
        TextKey.SearchEmptyBody to "Try a stop name or route number.",
    )

private val georgian =
    mapOf(
        TextKey.AppName to "Routy",
        TextKey.City to "ბათუმი • საზოგადოებრივი ტრანსპორტი",
        TextKey.Map to "რუკა",
        TextKey.Routes to "მარშრუტები",
        TextKey.Stops to "გაჩერებები",
        TextKey.Favorites to "რჩეულები",
        TextKey.Settings to "პარამეტრები",
        TextKey.Search to "მოძებნეთ მარშრუტი ან გაჩერება",
        TextKey.SearchRoutes to "მარშრუტის ნომერი ან სახელი",
        TextKey.SearchStops to "გაჩერების სახელი ან ნომერი",
        TextKey.Explore to "თქვენი ქალაქი, დაკავშირებული",
        TextKey.ExploreBody to "იპოვეთ თქვენი შემდეგი მგზავრობა ბათუმში",
        TextKey.AllRoutes to "ყველა მარშრუტი",
        TextKey.Nearby to "გაჩერებების ნახვა",
        TextKey.Retry to "ხელახლა ცდა",
        TextKey.Loading to "ტრანსპორტის მონაცემები იტვირთება…",
        TextKey.Empty to "ჯერ არაფერია",
        TextKey.NoResults to "ვერაფერი მოიძებნა. სცადეთ სხვა სახელი ან ნომერი.",
        TextKey.NoFavorites to "შეინახეთ მარშრუტები და გაჩერებები, რომ აქ იპოვოთ.",
        TextKey.Offline to "შენახული მონაცემები • განახლება მიუწვდომელია",
        TextKey.Refreshing to "მონაცემები ახლდება…",
        TextKey.NoInternet to "კავშირი არ არის. შეამოწმეთ ინტერნეტი და სცადეთ ხელახლა.",
        TextKey.Timeout to "კავშირს დიდი დრო დასჭირდა. სცადეთ ხელახლა.",
        TextKey.Server to "სატრანსპორტო სერვისი მიუწვდომელია. სცადეთ მოგვიანებით.",
        TextKey.Invalid to "მონაცემები ვერ წაიკითხა. სცადეთ განახლება.",
        TextKey.Storage to "ცვლილებები მოწყობილობაზე ვერ შეინახა.",
        TextKey.Unknown to "დაფიქსირდა შეცდომა. სცადეთ ხელახლა.",
        TextKey.Schedule to "დაგეგმილი გამგზავრება",
        TextKey.ScheduleNote to "ბათუმის დრო • განრიგი, არა რეალურ დროში პროგნოზი",
        TextKey.NextDeparture to "შემდეგი გასვლა",
        TextKey.Departure to "გასვლა",
        TextKey.NoSchedule to "განრიგი მიუწვდომელია",
        TextKey.Group to "გაჩერებების ჯგუფი",
        TextKey.Unspecified to "დაუზუსტებელი ჯგუფი",
        TextKey.Save to "შენახვა",
        TextKey.Saved to "შენახულია",
        TextKey.Remove to "წაშლა",
        TextKey.RemoveFavoriteTitle to "წაშალოთ შენახულებიდან?",
        TextKey.RemoveFavoriteBody to "მოგვიანებით შეგიძლიათ ისევ დაამატოთ.",
        TextKey.ShowMap to "რუკაზე ნახვა",
        TextKey.Back to "უკან",
        TextKey.Close to "დახურვა",
        TextKey.Live to "ავტობუსები რეალურ დროში",
        TextKey.Stale to "ბოლო ცნობილი მდებარეობები • განახლება მიუწვდომელია",
        TextKey.NoBuses to "ამ მარშრუტზე ავტობუსები არ დაფიქსირდა",
        TextKey.MyLocation to "ჩემი მდებარეობა",
        TextKey.LocationHelp to "მდებარეობა არასავალდებულოა და გამოიყენება მხოლოდ რუკაზე თქვენს საჩვენებლად.",
        TextKey.LocationUnavailable to "მდებარეობა მიუწვდომელია. შეამოწმეთ ნებართვები.",
        TextKey.Language to "ენა",
        TextKey.Appearance to "იერსახე",
        TextKey.ColorTheme to "ფერთა თემა",
        TextKey.Ocean to "Ocean",
        TextKey.Mint to "Mint",
        TextKey.Mono to "Mono",
        TextKey.System to "მოწყობილობის პარამეტრი",
        TextKey.Light to "ნათელი",
        TextKey.Dark to "მუქი",
        TextKey.English to "English",
        TextKey.Georgian to "ქართული",
        TextKey.Russian to "რუსული",
        TextKey.About to "ბათუმში ყოველდღიური მგზავრობისთვის",
        TextKey.MapUnavailable to "რუკა მიუწვდომელია. მარშრუტები და გაჩერებები ხელმისაწვდომია სიებში.",
        TextKey.FitRoute to "მარშრუტის ჩვენება",
        TextKey.Vehicle to "ავტობუსი",
        TextKey.Details to "დეტალები",
        TextKey.QuickSelect to "სწრაფი არჩევა",
        TextKey.SearchEmptyTitle to "ვერაფერი მოიძებნა",
        TextKey.SearchEmptyBody to "სცადეთ გაჩერების სახელი ან მარშრუტის ნომერი.",
    )

private val russian =
    mapOf(
        TextKey.AppName to "Routy",
        TextKey.City to "БАТУМИ • ОБЩЕСТВЕННЫЙ ТРАНСПОРТ",
        TextKey.Map to "Карта",
        TextKey.Routes to "Маршруты",
        TextKey.Stops to "Остановки",
        TextKey.Favorites to "Сохранённые",
        TextKey.Settings to "Настройки",
        TextKey.Search to "Найти маршрут или остановку",
        TextKey.SearchRoutes to "Номер или название маршрута",
        TextKey.SearchStops to "Название или номер остановки",
        TextKey.Explore to "Город на связи",
        TextKey.ExploreBody to "Найдите следующую поездку по Батуми",
        TextKey.AllRoutes to "Все маршруты",
        TextKey.Nearby to "Посмотреть остановки",
        TextKey.Retry to "Повторить",
        TextKey.Loading to "Загрузка данных транспорта…",
        TextKey.Empty to "Пока ничего нет",
        TextKey.NoResults to "Ничего не найдено. Попробуйте другое название или номер.",
        TextKey.NoFavorites to "Сохраняйте маршруты и остановки, чтобы быстро находить их здесь.",
        TextKey.Offline to "Сохранённые данные • обновления недоступны",
        TextKey.Refreshing to "Обновление данных транспорта…",
        TextKey.NoInternet to "Нет подключения. Проверьте интернет и повторите попытку.",
        TextKey.Timeout to "Подключение заняло слишком много времени. Попробуйте ещё раз.",
        TextKey.Server to "Сервис транспорта временно недоступен. Попробуйте позже.",
        TextKey.Invalid to "Не удалось прочитать данные транспорта. Попробуйте обновить.",
        TextKey.Storage to "Не удалось сохранить изменения на устройстве.",
        TextKey.Unknown to "Что-то пошло не так. Попробуйте ещё раз.",
        TextKey.Schedule to "Расписание отправлений",
        TextKey.ScheduleNote to "Время Батуми • расписание, а не прогноз прибытия",
        TextKey.NextDeparture to "Ближайшее отправление",
        TextKey.Departure to "Отправление",
        TextKey.NoSchedule to "Расписание недоступно",
        TextKey.Group to "Группа остановок",
        TextKey.Unspecified to "Не указано",
        TextKey.Save to "Сохранить",
        TextKey.Saved to "Сохранено",
        TextKey.Remove to "Удалить",
        TextKey.RemoveFavoriteTitle to "Удалить из сохранённых?",
        TextKey.RemoveFavoriteBody to "Позже его можно добавить снова.",
        TextKey.ShowMap to "Показать на карте",
        TextKey.Back to "Назад",
        TextKey.Close to "Закрыть",
        TextKey.Live to "Автобусы онлайн",
        TextKey.Stale to "Последние известные позиции • обновления недоступны",
        TextKey.NoBuses to "На этом маршруте нет данных об автобусах",
        TextKey.MyLocation to "Моё местоположение",
        TextKey.LocationHelp to "Местоположение необязательно и используется только для отображения на карте.",
        TextKey.LocationUnavailable to "Местоположение недоступно. Проверьте разрешения устройства.",
        TextKey.Language to "Язык",
        TextKey.Appearance to "Оформление",
        TextKey.ColorTheme to "Цветовая тема",
        TextKey.Ocean to "Океан",
        TextKey.Mint to "Мятная",
        TextKey.Mono to "Моно",
        TextKey.System to "Как на устройстве",
        TextKey.Light to "Светлая",
        TextKey.Dark to "Тёмная",
        TextKey.English to "English",
        TextKey.Georgian to "ქართული",
        TextKey.Russian to "Русский",
        TextKey.About to "Для ежедневных поездок по Батуми",
        TextKey.MapUnavailable to "Карта недоступна. Маршруты и остановки по-прежнему доступны в списках.",
        TextKey.FitRoute to "Показать маршрут",
        TextKey.Vehicle to "Автобус",
        TextKey.Details to "Подробнее",
        TextKey.QuickSelect to "Быстрый выбор",
        TextKey.SearchEmptyTitle to "Ничего не найдено",
        TextKey.SearchEmptyBody to "Попробуйте изменить название остановки или номер маршрута.",
    )
