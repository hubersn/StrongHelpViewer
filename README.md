# StrongHelpViewer

A viewer for RISC OS StrongHelp files written in Java Swing.

And also a converter for StrongHelp manuals into a bunch of HTML files for viewing in your favourite browser.

## Introduction

StrongHelp is the de-facto standard on RISC OS for online help. Introduced long before HTML and browsers became a thing, it offered a rich hypertext platform allowing nicely looking and nicely linked help content to be provided.

Originally developed by Guttorm Vik, the mastermind behind StrongEd (the IMHO best text editor on RISC OS), it is now maintained by Fred Graute (who, incidentally, also took over maintenance of StrongEd). See the [StrongEd website](http://www.stronged.iconbar.com/) for the current state of play.

One problem of StrongHelp content is of course its inherent lack of cross-platform availability. !StrongHlp, the StrongHelp viewer application, is written in ARM assembler, so does not really lend itself to being ported. A lot of RISC OS idiosyncrasies (from different font names to using the Sprite format for bitmap graphics and the Draw format for vector graphics and the Squash transparent file compression that is a non-standard LZW compression built into RISC OS) make access quite hard.

There were four solutions created over the past decades to make StrongHelp content available on other systems:
* StrawHelp by Rick Murray, a viewer application written in VisualBasic for Windows XP which uses an Internet Explorer view for visualization: [StrawHelp](https://heyrick.eu/software/strawhelp/)
* stronghelp-cgi by Vincent Sanders, written in fairly portable C, a CGI-based converter to provide on-the-fly translation of StrongHelp data into HTML in the context of a web server of your choice: [stronghelp-cgi](https://github.com/kyllikki/stronghelp-cgi)
* Shinto by Steve Drain, a converter running on RISC OS to produce HTML from a StrongHelp manual; it is currently unreleased
* Jongware StrongHelp, a viewer application for Windows with integrated keyword search and a direct-render approach (i.e. no HTML involved here!) including a basic Draw renderer: currently unreleased, a description and a screenshot can be found here: [Jongware StrongHelp](http://www.jongware.com/stronghelp.html)

All four approaches have many limitations - two of them mainly one: they are not freely available - and dodge various complexities of the StrongHelp format as well as being rather unfriendly to use for your average just-let-me-start-that-app-to-view-my-StrongHelp-content computer user (i.e. me and many others).

StrongHelpViewer to the rescue - a bunch of Java code (Java 7 compatible, so also for Windows XP if you like, but completely cross-platform for any later Windows version or Linux or MacOS) to do two things: it lets you view StrongHelp content with a graphical UI, and it lets you convert StrongHelp content into a bunch of HTML files to be put on a web server and viewed with your web browser of choice.

## How to use

There is no nicely-packaged distribution yet, so compile-yourself. This repo is a ready-made Eclipse project, but since it lacks any complicated 3rd party dependencies, you should be able to use it in any IDE you like, including vi-with-javac. Just add the compiled code as well as the content of the res directory to your classpath and everything should just work.

Run com.hubersn.riscos.stronghelp.StrongHelp for the viewer (if no parameters are given, it will open a filechooser letting you select either a directory with StrongHelp manuals, a single StrongHelp manual or a ZIP file with a StrongHelp manual inside) and com.hubersn.riscos.stronghelp.StrongHelpConverter for the converter. The StrongHelpConverter has "usage" output that will guide you.

You need at least Java 7 to compile and run.

An easy example: if you have a collection of StrongHelp manuals (like all of us), put them into one directory and use (Windows example)

``com.hubersn.riscos.stronghelp.StrongHelp C:\data\this\path\to\the\manual\directory``

to arrive at a UI that does not look completely unlike the original !StrongHlp.

## The Code - How it works

The code can be separated into four areas:
* interpretation of StrongHelp's image file format - every StrongHelp manual, despite being a single file, is structured as a filing system, known on RISC OS as an "image filing system" that is basically a file, but as soon as for this filetype an image filing system is registered, it suddenly becomes a directory containing files and possibly other directories.
* conversion of StrongHelp content into HTML with a bit of CSS
* providing a minimal UI in Java Swing to show the resulting HTML in a window (which uses standard JEditorPane, which means that the complexity of the used HTML must match the JEditorPane/HTMLEditorKit capabilities)
* providing a minimal CLI shell to convert manuals into HTML

There are two main entry points for the two use cases:
* com.hubersn.riscos.stronghelp.StrongHelp is the viewer application that allows you to view either one StrongHelp manual (even if inside a ZIP archive) or a directory of StrongHelp manuals
* com.hubersn.riscos.stronghelp.StrongHelpConverter is the converter application that allows you to convert one StrongHelp manual or a directory of StrongHelp manuals into HTML for viewing with your web browser of choice

Note that no care has been taken to arrive at a really fast solution. The parsing is simple-minded. There is no cacheing. The code is straight-forward without employing any clever tricks to speed things up. Well, it DOES use StringBuilder where possible, but that's it.

Also note that I have really no idea how to properly write that scanner-parser-model-transform-to-output stuff. It is ad-hoc code. Don't even start to think that it is the way you should write such stuff.

The few classes that do not live in the com.hubersn.riscos.stronghelp package and its subpackages are taken from my various in-house libs that have not yet been published in a formal way. No fancy stuff in there, just some convenience code without pulling in mebibytes of over-complicated 3rd party dependencies. It is pretty much guaranteed that the code provided here is so simple that it will still work with Java 23 (coming soon!). And with a bit of effort, you could certainly convert it down to Java 2 (or 1.2 as we old-timers say).

## Cool features

At the moment, mostly one: you can directly choose a ZIP file, and if it contains a StrongHelp manual, it will be opened directly. All other things are very basic, rudimentary, buggy, unreliable...

## Limitations

* no support for Sprites (will be provided later by using my SpriteConverter code)
* no support for Squashed content (will be provided later by using code from James Woodcock's riscosarc project)
* no support for Draw files (might be added later, e.g. by translating Draw into SVG and let the browser or svgSalamander worry about displaying it)
* styles defined on page-level not properly supported
* fonts with different width-height values not supported
* font mapping is currently hardcoded, true RISC OS fonts are not usable
* no true Acorn Latin codepage support, WINDOWS-1252 is instead used to interpret the text - this is a problem for special characters, see Basalt manual for examples
* various page commands are either unsupported or have only limited support or have an alternative implementation
    * #Below will only make sense once images are supported
    * #Bottom makes no sense in an HTML context
    * #Subpage sections are not split into separate views, but visualized all on the main page, with intra-page links to jump to
    * #Background is partly supported in its WIMP colour and RGB colour syntax, but not for Sprite tiling, and WIMP 1 will not use the marbled background
    * stacked behaviour of #Align and #Indent and #F and #RGB is not supported
* things that require a full RISC OS running for a sensible level of support are not implemented
    * execution of bundled Utility code is not possible
    * replacement of OS variables is not possible
* no search capability
* Adjust-Click page history missing
* FileType help by drag-file-to-StrongHelp missing
* Shift-Select "type that word" feature missing
* #Pointer command is not supported
* NotFound_ semantics only supported for Viewer application, not yet in the HTML converter part
* old-style cross-manual links not supported

One nice thing about RISC OS StrongHelp is its integration into other applications for help lookup. Other than providing an API to do the same with StrongHelpViewer, I am not sure how to improve in this respect.

## The future

Fix most the limitations...and possibly provide a package that either runs standalone as an HTTP(S) server, or can run in any Servlet container.

Or even provide a server somewhere running StrongHelpViewer where you can upload your own StrongHelp manuals as well as accessing many well-known manuals.

## History

Why I started that project, I don't remember. I hate data formats that I cannot use on every system I like and/or need to use, and always firing up RPCEmu just to have a quick look at a StrongHelp manual is not a nice solution. So I looked for existing approaches, and found them lacking. Since reinventing the wheel is the base of all IT, I started this project.

You might find a pattern here, other forthcoming projects of mine like FilecoreImageReader or SpriteConverter or BBCBasicDetokenizer have a very similar theme.

## License

The code is licensed under "The Unlicense". This is an attempt to give any user a maximum amount of freedom. Basically it means "do-what-you-like, but it remains my copyright, so you cannot pretend that you created it".
