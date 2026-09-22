<div align="center">

<a href="https://git.io/typing-svg"><img src="https://readme-typing-svg.demolab.com?font=Manrope&weight=700&size=38&pause=1000&color=EB2544&background=FFFFFF00&center=true&vCenter=true&width=700&lines=React+AntiCheat;Minecraft+Cheat+Protection" alt="React Typing SVG"/></a>

<p>Защита основанная на AI аналитике поворотов</p>

<p>
  <a href="https://github.com/g4vrk/React/actions/workflows/gradle-publish.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/g4vrk/React/build.yml?style=flat&label=build&logo=github&logoColor=white" alt="Build">
  </a>
  <a href="https://jitpack.io/#g4vrk/React">
    <img src="https://img.shields.io/jitpack/version/com.github.g4vrk/React?style=flat&logo=jitpack&logoColor=white" alt="JitPack">
  </a>
  <a href="https://github.com/g4vrk/React/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/g4vrk/React?style=flat&logo=opensourceinitiative&logoColor=white" alt="License">
  </a>
    <br>
  <a href="https://github.com/g4vrk/React/stargazers">
    <img src="https://img.shields.io/github/stars/g4vrk/React?style=flat&logo=githubsponsors&logoColor=white" alt="Stars">
  </a>
  <a href="https://github.com/g4vrk/React/network/members">
    <img src="https://img.shields.io/github/forks/g4vrk/React?style=flat&logo=git&logoColor=white" alt="Forks">
  </a>
</p>

<p>
  <a href="https://react-ac.space">Сайт</a>
  &nbsp;•&nbsp;
  <a href="https://t.me/react_ac">Канал</a>
  &nbsp;•&nbsp;
  <a href="https://t.me/react_ac_support">Поддержка</a>
</p>

</div>

---

> [English version](README.en.md)

## Скачивание плагина

- Последние обновления:
    - **[GitHub Tags](https://github.com/g4vrk/react/tags)** *(рекомендуется)*
    - **[SpigotMC RU](https://spigotmc.ru/resources/react-besplatnyj-antichit-s-ii-proverkami.5997/updates)**

---

## Требования & Установка

- Версия сервера: `1.16.5 - 26.2`
- Ядро сервера: `Paper / Folia`

1. Скачайте `.jar` файл плагина
2. Переместите его в папку `plugins/`
3. После запуска настройте `inference.yml` для подключения внешнего сервера

> [!IMPORTANT]
> Если вы используете нашу модель на сайте https://www.react-ac.space/, то настраивать ничего не нужно - просто скачайте
> готовый inference.yml с сайта и замените его в папке плагина

---

## Сборка исходников

1. `git clone https://github.com/g4vrk/React.git`
2. `cd React`
3. `./gradlew build`
4. Готовые файлы плагина будут лежать в папках `<ядро>/build/libs`

---

## Как работает анализ?

- Суть плагина в том, что он лишь отправляет запросы на внешний настроенный сайт, вы можете использовать наш
  сайт https://www.react-ac.space/ или же сделать свою модель и использовать ее.
- После получения ответа на запрос анализа, плагин работает с этими данными, для того чтобы мы могли смотреть
  статистику / историю аналитики игроков, так же эти данные сохраняются и доступны в аддонах.
- То что делать с данными уже исключительно ваше решение, можете банить, можете кикать, можете просто проверять вручную,
  но суть плагина - облегчить задачу

---

## Структура

| Модуль   | Описание                                           |
|----------|----------------------------------------------------|
| `common` | Главная работа античита, данные, проверки и прочее |
| `paper`  | Реализация для Paper серверов                      |
| `folia`  | Реализация для Folia серверов                      |

---

## API для разработчиков

> [!IMPORTANT]
> Если вы хотите добавить в плагин какую либо функцию / механику, то вы можете создать аддон, помощь в их создании
> оказывает https://t.me/g4vrk/

<details>
<summary><b>Подключение через JitPack</b></summary>

<br>

<div align="center">

<b>Gradle (Kotlin DSL)</b>

</div>

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.g4vrk.React:react-common:VERSION")
}
```

<div align="center">

<b>Maven</b>

</div>

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.g4vrk.React</groupId>
    <artifactId>react-common</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

</details>

---

## Лицензия

Плагин распространяется под MIT — см. [LICENSE](LICENSE).