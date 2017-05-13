# Github Release Watcher

> A simple Scala.js app to list releases in for one or more projects in Github.

The app authenticates with your Github account to fetch the releases from the Github API. The release data is displayed in a [<vaadin-grid>](https://vaadin.com/elements/-/element/vaadin-grid).

## Demo
See the app in action at [vaadin-release-watcher.firebaseapp.com](https://vaadin-release-watcher.firebaseapp.com/) 

## Configuration
Alter the `organization` and `repos` vals in App object to choose which repositories to look at.

## Buildingand running
This is a SBT project.

``` bash
# Start SBT
$ sbt

# Start a loop that builds the project whenever sources change
sbt> ~fastOptJS
```

You need to serve the project directory over HTTP to access it with your browser. Eg. with Python:
```
$ python -c "import BaseHTTPServer as bhs, SimpleHTTPServer as shs; bhs.HTTPServer((\"127.0.0.1\", 8888), shs.SimpleHTTPRequestHandler).serve_forever()"
```
After this the UI is accessible at [http://localhost:8888/index-dev.html](http://localhost:8888/index-dev.html)
