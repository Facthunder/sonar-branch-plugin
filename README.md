# SonarQube Branch Plugin
SonarQube is an open platform to manage code quality. This plugin activate ability to analyze other branch than master branch, it is an open source and independent version of the commercial versions of SonarQube.

This plugin is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.

### Quickstart
- Setup a SonarQube instance
- Install the plugin in <sonar_install_dir>/extensions/plugins
- Start SonarQube
- Run an analysis by specifying `sonar.branch.name` property

### Features
- Run analysis on different branches

### Configuration
- Sign-in as an administrator
- Set following properties
  - `sonar.branch.longLivedBranches.regex`: set the regular expression to recognize long living branches 
  - `sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches`: set time before deleting short living branches

### How to contribute
If you experienced a problem with the plugin please open an issue. Inside this issue please explain us how to reproduce this issue and paste the log.

If you want to do a PR, please put inside of it the reason of this pull request. If this pull request fix an issue please insert the number of the issue or explain inside of the PR how to reproduce this issue.

### License
Copyright 2019 Facthunder.

Licensed under the [GNU General Public License, Version 3.0](https://www.gnu.org/licenses/gpl.txt)
