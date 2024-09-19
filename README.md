# From Polly:
### Quick Note:

I had to integrate the SDK in order to actually get the API calls, so sorry...

## To get git and this repo set up

First, open any project window. Then, click VCS on the top task bar, then "Enable Version Control Integration". "Git" is what you want, and click okay.

Now, click Git in the taskbar, clone, and paste our group github URL (you can also sign in here; hint: you should do that). When you click clone, it will open our project.

You can create new branches and add and merge and pull or whatever from the git tab whenever.

# From myScript: 
## Building your own integration

This repository provides you with a ready-to-use reference implementation of the Android integration part, covering aspects like ink capture and rendering. It is located in `UIReferenceImplementation` directory and can be simply added to your project by referencing it in your `settings.gradle`.

## Documentation

A complete guide is available on [MyScript Developer website](https://developer.myscript.com/docs/interactive-ink/latest/android/).

The API Reference is available directly in Android Studio once the dependencies are downloaded.

## Getting support

You can get some support from the dedicated section on [MyScript Developer website](https://developer.myscript.com/support/).

## Troubleshoot

If you encounter build errors of the form `No version of NDK matched the requested version`, please install the requested NDK version or update the one referenced in `build.gradle` to match your installed NDK version. You can follow [these instructions](https://developer.android.com/studio/projects/install-ndk#specific-version).

## Sharing your feedback ?

Made a cool app with Interactive Ink? Ready to cross join our marketing efforts? We would love to hear about you!
We’re planning to showcase apps using it so let us know by sending a quick mail to [myapp@myscript.com](mailto://myapp@myscript.com).

## Contributing

We welcome your contributions:
If you would like to extend those examples for your needs, feel free to fork it!
Please sign our [Contributor License Agreement](CONTRIBUTING.md) before submitting your pull request.
