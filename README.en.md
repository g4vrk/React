<div align="center">

<a href="https://git.io/typing-svg"><img src="https://readme-typing-svg.demolab.com?font=Manrope&weight=700&size=38&pause=1000&color=EB2544&background=FFFFFF00&center=true&vCenter=true&width=700&lines=React+AntiCheat;Minecraft+Cheat+Protection" alt="React Typing SVG"/></a>

<p>Protection powered by AI-based rotation analysis</p>

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
  <a href="https://react-ac.space">Website</a>
  &nbsp;•&nbsp;
  <a href="https://t.me/react_ac">Channel</a>
  &nbsp;•&nbsp;
  <a href="https://t.me/react_ac_support">Support</a>
</p>

</div>

---

> [Русская версия](README.md)

## Download

* Latest releases:

    * **[GitHub Tags](https://github.com/g4vrk/react/tags)** *(recommended)*
    * **[SpigotMC RU](https://spigotmc.ru/resources/react-besplatnyj-antichit-s-ii-proverkami.5997/updates)**

---

## Requirements & Installation

* Server versions: `1.16.5 - 26.2`
* Server software: `Paper / Folia`

1. Download the plugin `.jar` file.
2. Move it to the `plugins/` folder.
3. After starting the server, configure `inference.yml` to connect to an external analysis server.

> [!IMPORTANT]
> If you are using our model through https://www.react-ac.space/, no additional configuration is required. Simply
> download the ready-to-use `inference.yml` from the website and replace the file in the plugin folder.

---

## Building from Source

1. `git clone https://github.com/g4vrk/React.git`
2. `cd React`
3. `./gradlew build`
4. The built plugin files will be available in `<platform>/build/libs`.

---

## How Does the Analysis Work?

* React sends analysis requests to an externally configured server. You can use our service
  at https://www.react-ac.space/ or deploy and use your own model.
* After receiving the analysis response, the plugin processes the returned data so that player statistics and analysis
  history can be tracked. The same data is also stored and made available to addons through the API.
* What you do with the results is entirely up to you. You can ban players, kick them, or simply review the data
  manually. The main purpose of React is to simplify this process.

---

## Project Structure

| Module   | Description                                   |
|----------|-----------------------------------------------|
| `common` | Core anti-cheat logic, data, checks, and more |
| `paper`  | Implementation for Paper servers              |
| `folia`  | Implementation for Folia servers              |

---

## Developer API

> [!IMPORTANT]
> If you want to add your own functionality or mechanics to the plugin, you can create an addon. Help with addon
> development is available at https://t.me/g4vrk/

<details>
<summary><b>Using JitPack</b></summary>

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

## License

The plugin is distributed under the MIT License. See [LICENSE](LICENSE).
