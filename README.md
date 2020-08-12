# STEP capstone

**Welcome to AI-ssistant!**

[Let's Start a Presentation!](https://drive.google.com/file/d/14-kxNo-px6-BjpMXyMRoJf6IbeqmbsY2/view?usp=sharing)

AI-ssistant is a web-based personal assistant application that supports common Google Assistant tasks, as well as unique add-on features. Our web application aims to make assistant technologies easily accessible to all users through a web browser, rather than a smart device. Additionally, by providing both textual and audio interactions, we hope to support a wider audience with varying accessibility levels. Our ultimate goal is to create an easy online tool that provides useful services, organizes and shares information, and performs helpful and supportive tasks. We designed and developed the AI-ssistant during our STEP internship at Google, Summer 2020.

We created this full-stack web application using a variety of Google APIs and services, including: Dialogflow, Text To Speech, Speech to Text, Translate, Google Maps, Timezone, Geocoding, OAuth 2.0, People API, Google Books API, Youtube Data API, UserService, and Datastore. AI-ssistant is written in Java, HTML, CSS, and JavaScript. We used Dialogflow, a natural understanding platform, to create intents, entities, and training phrases. The Dialogflow folder contains this data. From our inputted training phrases, Dialogflow is able to detect user intent from a user inputted audio stream. Based on the detected intent, our system determines which agent should handle creating the apporpriate fulfilment, which is sent to the Text to Speech API and outputted to the user, and display information to be placed on the user interface.


**Standard Assistant Features:**

[MVP Demo](https://drive.google.com/file/d/1Z4b51Q4gQNconXoqCp40vSlvPqqmQnIr/view?usp=sharing)

[Currency Converter Demo](https://drive.google.com/file/d/1vv7TdeK9xY4HUkZ4U3gABweLTTiZZRIM/view?usp=sharing)

[Date Feature Demo](https://drive.google.com/file/d/1IXy-AIu2iDR3VhrodWOOZVCyZU_WJGww/view?usp=sharing)

[Language Change Demo](https://drive.google.com/file/d/1PVdw4ppLtQXUsprxqGoxdb7pYnmrpAlI/view?usp=sharing)

[Maps Demo](https://drive.google.com/file/d/1zRA-nX9D79xKZZ1vqKPNxZXAEgph1qsf/view?usp=sharing)

[Name Change Demo](https://drive.google.com/file/d/1C7MukFIrGJJVbYyATl1iyr5RtFPOukuW/view?usp=sharing)

[Time Feature Demo](https://drive.google.com/file/d/1-kmvyrTo_TRQU8v8WJIQ5dR7_1plZzi2/view?usp=sharing)

[Timer Demo](https://drive.google.com/file/d/15HQN-ZJMxnCwONlNKTP_VWLe98I1JdqP/view?usp=sharing)

[Tip Demo](https://drive.google.com/file/d/1yCsl45LGFGwegbnvi_d7vzBsfBUbQGe2/view?usp=sharing)

[Translate Demo](https://drive.google.com/file/d/1SacV1KONbigi_tAvm8CEJbfk3v3LR9q5/view?usp=sharing)

[Unit Converter Demo](https://drive.google.com/file/d/11BBjnG28tKJovYAXmte3cRwSfj56JHjG/view?usp=sharing)

[Weather Feature Demo](https://drive.google.com/file/d/1icfUNumgtVbamJtELaAaEoDluTUMCsxw/view?usp=sharing)

[Web Search Feature Demo](https://drive.google.com/file/d/1ipHydTNTZSpYnZpHDiSj15iwlMtyys2D/view?usp=sharing)


**Add-On Features:**

The **Books Feature** creates a social books experience that allows users to discover and share books with their friends through our AI-ssistant. The Books Feature has four main functionalities. First, users can like books, browse through a feed of their friends’ liked books, and navigate to users’ profiles to see their likes. Second, users can perform searches to discover books that match their specifications, filtering by title, category, language, type (magazine, book, etc.), ebook (paid or free), author, or bookshelf. Third, for each book displayed by the AI-ssistant, users can request for a description or an embedded preview of the book, allowing them to learn more about the book directly on the AI-ssitant interface. Finally, our assistant can manage users’ Google Books accounts by adding books to or deleting books from their private bookshelves.

[Books Feature Demo](https://drive.google.com/file/d/1tUzpzXgt7OvJrJtQSi9VyRZD95snOUja/view?usp=sharing)

The **Workout Feature** helps users workout in three main way. The first is by helping users find workout videos. Uses can specify workout type, YouTube channel, or workout length to get relevant results. They can also save these videos to their workout dashboard and new more pages of videos. The second is creating workout plans for the user. Users can ask the assistant to generate a workout plan by specifying workout plan length and workout type and the assistant generates a workout plan that they can save to their dashboard. Finally, the workout agents gives logged in users access to their workout dashboard. Here, users can also see all of their saved workout content (saved videos and workout plans) as well as a graph of their recently tracked activity minutes. When users click on specific workout plans, they can track progress for each day by marking the workout as done.

[Workout Feature Demo](https://drive.google.com/file/d/1C6QG1zplKnywT1L89OAn4Xq_uVcidPqN/view?usp=sharing)

The **Memory Feature** serves as an agent that provides useful information specifically curated for users based on their interaction history with the assistant. Specifically, it provides two main functionalities: allowing queries to conversation history if the user wanted to remember something they talked about in the past and providing an easily accessible method for storing ideas or items into lists. The first functionality is a user-triggered history search that responds to requests for keywords and/or time periods, for which the assistant will respond by showing the user all instances of a keyword showing up in their conversation history along with contextual surrounding conversation or all comments within a specified timeframe. The latter feature allows the user to easily create, update, and view lists. Additionally, for popular lists, the system will provide item recommendations to add based on either the user's personal list item preferences or predicted user interest in comparison to other similar user preferences. Ultimately, the goal of this recommendation feature is to make the list creation process easier and also introduce the user to discover new items of interest.

[Memory Feature Demo](https://drive.google.com/file/d/1FJpm9f7twSPh0cESiH6ed1bPoVtsu6Xz/view?usp=sharing)

By: Abby Mapes, Mihira Patel, Angela Liu
