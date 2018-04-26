Zemberek-NLP
============

Zemberek-NLP provides basic Natural Language Processing tools for Turkish.
Code and API is **not** compatible with the old [Zemberek2](https://github.com/ahmetaa/zemberek) project. 
Please note that **all code and APIs are subject to change drastically until version 1.0.0**

Latest version is 0.11.1 (April 13th 2017). [Change Log](CHANGELOG.md)

## Citing

If you use this project in an academic publication, please refer to this site.

## FAQ 

Please read the [FAQ](https://github.com/ahmetaa/zemberek-nlp/wiki/FAQ) for common questions.

## Usage

### Maven

Add this to pom.xml file

    <repositories>
        <repository>
            <id>ahmetaa-repo</id>
            <name>ahmetaa Maven Repo on Github</name>
            <url>https://raw.github.com/ahmetaa/maven-repo/master</url>
        </repository>
    </repositories>

And dependencies (For example morphology):

    <dependencies>
        <dependency>
            <groupId>zemberek-nlp</groupId>
            <artifactId>morphology</artifactId>
            <version>0.11.0</version>
        </dependency>
    </dependencies>

### Jar distributions

[Google docs page](https://drive.google.com/#folders/0B9TrB39LQKZWSjNKdVcwWUxxUm8) page 
contains jar files for different versions. 

[**module-jars**] folder contain all zemberek modules as separate jar files. 
[**zemberek-all-VERSION.jar**] contains all zemberek modules. [**dependencies**] folder contains other dependencies suchas Google Guava.

### Examples

There is a [examples](examples) module in the code for usage examples.

Also, there is a separate project with same examples that uses Zemberek-NLP as maven modules: 
[Turkish-nlp-examples](https://github.com/ahmetaa/turkish-nlp-examples)

## Modules

### Core

Core classes such as special Collection classes, Hash functions and helpers.

Maven artifact id : **core**

### Morphology

Turkish morphological analysis, disambiguation and word generation. [Documentation](morphology)

Maven artifact id : **morphology**

### Tokenization

Turkish Tokenization and sentence boundary detection. [Documentation](tokenization)

Maven artifact id : **tokenization**

### Language Identification.

Allows fast identification of text language. [Documentation](lang-id)

Maven artifact id : **lang-id**

### Language modeling

Provides a language compression algorithm. [Documentation](lm)

Maven artifact id : **lm**

### Normalization

Provides spell checker and suggestion functions. [Documentation](normalization)

Maven artifact id : **normalization**

## Known Issues and Limitations
- Project requires Java 8.
- Currently word and sentence parse module operations generates parse graph with each initialization.
So each run in the system takes some seconds. 
- Morphological parsing does not work for some obvious and frequent words.
- Morphological disambiguation is working less accurate then expected.
- Morphological generation may not work for some obvious Stem-Suffix combinations.
- Please see issues section for further issues and feel free to create new ones.

## License
Code is licensed under Apache License, Version 2.0

## Acknowledgements
Please refer to contributors.txt file.
