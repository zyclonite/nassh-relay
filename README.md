[![CircleCI](https://circleci.com/gh/zyclonite/nassh-relay.svg?style=svg)](https://circleci.com/gh/zyclonite/nassh-relay)

# nassh-relay

Relay Server for the Secure Shell Chromium plugin

## Documentation

See https://github.com/zyclonite/nassh-relay/wiki/Manual for details

## Docker

```
$ docker run -d --name nassh-relay -p 8022:8022 zyclonite/nassh-relay
```

## Demo

[![PlayWithDocker](https://github.com/play-with-docker/stacks/raw/cff22438cb4195ace27f9b15784bbb497047afa7/assets/images/button.png)](http://play-with-docker.com?stack=https://gist.githubusercontent.com/zyclonite/2351c74e618382486855683b8ff749e6/raw/5200ea23f0448333f8a71907fce8f1391d7f5547/nassh-stack.yml)

client options (fill in your temp docker url from pwd)
```
--proxy-host=pwd10-XXX-XXX-XXX-8022.host1.labs.play-with-docker.com --proxy-port=80
```

## Source

The project's source code is hosted at:

https://github.com/zyclonite/nassh-relay

## Github procedures

nassh-relay accepts contributions through pull requests on GitHub. After review a pull
request should either get merged or be rejected.

When a pull request needs to be reworked, say you have missed something, the pull
request is then closed, at the time you finished the required changes you should
reopen your original Pull Request and it will then be re-evaluated. At that point if
the request is aproved we will then merge it.

Make sure you always rebase your branch on master before submitting pull requests.
