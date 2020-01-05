GnuCash Android is built by people like you! Please [join us](https://github.com/codinguser/gnucash-android).

## Reporting Issues
* The GitHub issue tracker is used for collecting and managing bugs, feature requests and general development planning.
* When creating a request, first search to make sure a similar one doesn't already exist in the tracker. 
* Be as specific as possible when providing descriptions of the problems encountered and what the expected behaviour should be.
* It is also possible to report issues by creating tickets directly from within the app (in the Help Center)

## Code Contributions
* Contributions are submitted, reviewed, and accepted using Github pull requests. [Read this article](https://help.github.com/articles/using-pull-requests) for some details. We use the _Fork and Pull_ model, as described there.
* You can maintain your stable installation of GnuCash and test with another installation.
The two instances of GnuCash Android will live side-by-side on your device and not affect each other. You can install the development version by executing `gradlew installDevelopmentDebug` inside the root project directory
* The latest changes are in the `develop` branch.
  * Always rebase develop before working on a fix or issuing a pull request
* The master branch contains only stable releases.
  * Pull requests to the `master` branch will be rejected.
* The `hotfix/patches` branch is reserved for very small fixes to the current release
  * This branch may diverge significantly from the `develop` branch
  * When working on a hotfix, always rebase and start off the `origin/hotfix/patches` branch
  * Examples of such are typos, translation updates, critical bugs (e.g. cannot save transactions)
  * Any bigger changes should be made to develop

* Make a new branch for every feature you're working on.
* Try to make clean commits that are easily readable (including descriptive commit messages!)
* Test before you push make sure all test pass on your machine.
  * Unit tests can be run with `gradle test`
  * UI tests can be run with `gradle spoonDD`. This will run the tests on all connected devices/emulators.
* Make small pull requests that are easy to review but which also add value.

## Coding style
* Do write comments. You don't have to comment every line, but if you come up with something thats a bit complex/weird, just leave a comment. Bear in mind that you will probably leave the project at some point and that other people will read your code. Undocumented huge amounts of code are nearly worthless!
* Please make sure to document every method you write using Javadoc, even if the method seems trivial to you
  * See [this guide](http://www.oracle.com/technetwork/articles/java/index-137868.html) on how to write good Javadoc comments
* Don't overengineer. Don't try to solve any possible problem in one step, but try to solve problems as easy as possible and improve the solution over time!
* Do generalize sooner or later! (if an old solution, quickly hacked together, poses more problems than it solves today, refactor it!)
* Keep it compatible. Do not introduce changes to the public API, or configurations too lightly. Don't make incompatible changes without good reasons!

## Translation
* Tranlations for GnuCash Android are managed using [CrowdIn](crowdin.com/project/gnucash-android)
* You can sign up for an account and create/vote for translations.
* Translations will not be accepted via pull requests

## Documentation
* Documentation should be kept up-to-date. This means, whenever you add a new API method, add a new hook or change the database model, pack the relevant changes to the docs in the same pull request.
