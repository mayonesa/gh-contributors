package services

import exceptions.{Gh404ResponseException, OtherThanGh404ErrorException}
import models.ContributorInfo
import org.scalatest.matchers.dsl.ResultOfATypeInvocation
import org.scalatestplus.play._
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.sird._
import play.api.test._
import play.core.server.Server

import scala.concurrent.Await
import scala.concurrent.duration._

class GitHubSpec extends PlaySpec {
  "`contributorsByNCommits`" must {
    import scala.concurrent.ExecutionContext.Implicits.global
    import Results._

    val empty = Json.arr()
    val orgName0 = "org_name"
    val perPage0 = 42

    def testHappy(serverMock: DefaultActionBuilder => PartialFunction[RequestHeader, Handler])
                 (assertContributors: Vector[ContributorInfo] => Unit): Unit =
      testWithServerMock(serverMock) { gh =>
        val contributors = Await.result(gh.contributorsByNCommits(orgName0), 10.seconds)
        assertContributors(contributors)
      }

    def testException[T <: Exception](serverResponse: Status, exception: ResultOfATypeInvocation[T]): Unit = {
      testWithServerMock(Action => {
        case GET(p"/orgs/$_/repos") => Action(serverResponse)
      }) { gh =>
        exception must be thrownBy {
          Await.result(gh.contributorsByNCommits(orgName0), 10.seconds)
        }
      }
    }

    def testWithServerMock(serverMock: DefaultActionBuilder => PartialFunction[RequestHeader, Handler])
                          (forContributors: GitHub => Unit): Unit =
      Server.withRouterFromComponents() { components =>
        serverMock(components.defaultActionBuilder)
      } { implicit port =>
        WsTestClient.withClient { client =>
          val gh = new GitHub(client, "", "", "", perPage0.toString)
          forContributors(gh)
        }
      }

    "no repos w/ all req'd params" in {
      testHappy(Action => {
        case GET(p"/orgs/$orgName/repos" ? q"page=${int(page)}" & q"per_page=${int(perPage)}") =>
          orgName mustBe orgName0
          page mustBe 1
          perPage mustBe perPage0
          Action(Ok(empty))
      })(_.isEmpty mustBe true)
    }
    "no contributors" in {
      val repoName = "repo_name"
      val owner = "owner"

      testHappy(action => {
        case GET(p"/orgs/org_name/repos" ? q"page=${int(page)}") =>
          action {
            val first = Json.parse(
              s"""
              [
                {
                  "id": 1296269,
                  "name": "$repoName",
                  "full_name": "octocat/Hello-World",
                  "owner": {
                    "login": "$owner",
                    "id": 1
                  },
                  "private": false,
                  "html_url": "https://github.com/octocat/Hello-World"
                },
                {
                  "id": 1296270,
                  "name": "repo_name0",
                  "full_name": "octocat/Hello-World",
                  "owner": {
                    "login": "owner_0",
                    "id": 1
                  },
                  "private": false,
                  "html_url": "https://github.com/octocat/Hello-World"
                }
              ]""")
            Ok(if (page == 1) first else empty)
          }
        case GET(p"/repos/$_/$_/contributors") => action(Ok(empty))
      })(_.isEmpty mustBe true)
    }
    "multi-repos w/ multi-contributors" in {
      val repoName1 = "repo_name1"
      val owner1 = "owner1"
      val repoName2 = "repo_name2"
      val owner2 = "owner2"
      val name1 = "name1"
      val c1 = 40
      val ci1 = ContributorInfo(name1, c1)
      val name2 = "name2"
      val c2 = 35
      val ci2 = ContributorInfo(name2, c2)
      val name3 = "name3"
      val c3 = 26
      val ci3 = ContributorInfo(name3, c3)
      val name4 = "name4"
      val c4 = 23
      val ci4 = ContributorInfo(name4, c4)
      val c3_1 = 5
      val ci3_1 = ContributorInfo(name3, c3_1)
      val repo1Contributors = Json.parse(
        s"""
           |[
           |  {
           |    "login": "${ci1.name}",
           |    "id": 1,
           |    "node_id": "MDQ6VXNlcjE=",
           |    "avatar_url": "https://github.com/images/error/octocat_happy.gif",
           |    "gravatar_id": "",
           |    "url": "https://api.github.com/users/octocat",
           |    "html_url": "https://github.com/octocat",
           |    "followers_url": "https://api.github.com/users/octocat/followers",
           |    "following_url": "https://api.github.com/users/octocat/following{/other_user}",
           |    "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
           |    "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
           |    "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
           |    "organizations_url": "https://api.github.com/users/octocat/orgs",
           |    "repos_url": "https://api.github.com/users/octocat/repos",
           |    "events_url": "https://api.github.com/users/octocat/events{/privacy}",
           |    "received_events_url": "https://api.github.com/users/octocat/received_events",
           |    "type": "User",
           |    "site_admin": false,
           |    "contributions": ${ci1.contributions}
           |  },
           |  {
           |    "login": "${ci3.name}",
           |    "contributions": ${ci3.contributions}
           |  },
           |  {
           |    "login": "${ci3.name}",
           |    "contributions": ${ci3.contributions}
           |  },
           |  {
           |    "login": "${ci4.name}",
           |    "contributions": ${ci4.contributions}
           |  },
           |  {
           |    "login": "${ci3_1.name}",
           |    "contributions": ${ci3_1.contributions}
           |  }
           |]
           |""".stripMargin)
      val repo2Contributors = Json.parse(
        s"""
           |[
           |  {
           |    "login": "${ci1.name}",
           |    "contributions": ${ci1.contributions}
           |  },
           |  {
           |    "login": "${ci2.name}",
           |    "contributions": ${ci2.contributions}
           |  }
           |]
           |""".stripMargin)

      testHappy(Action => {
        case GET(p"/orgs/org_name/repos" ? q"page=${int(page)}") =>
          Action {
            val firstPageRepos = Json.parse(
              s"""
              [
                  {
                    "id": 1296269,
                    "name": "$repoName1",
                    "full_name": "octocat/Hello-World",
                    "owner": {
                      "login": "$owner1",
                      "id": 1
                    },
                    "private": false,
                    "html_url": "https://github.com/octocat/Hello-World"
                  }
                ]""")
            val secondPageRepos = Json.parse(
              s"""
                [
                  {
                    "id": 1296270,
                    "name": "$repoName2",
                    "full_name": "octocat/Hello-World",
                    "owner": {
                      "login": "$owner2",
                      "id": 1
                    },
                    "private": false,
                    "html_url": "https://github.com/octocat/Hello-World"
                  }
                ]""")
            Ok(page match {
              case 1 => firstPageRepos
              case 2 => secondPageRepos
              case 3 => empty
            })
          }
        case GET(p"/repos/$owner/$repoName/contributors" ? q"page=${int(page)}") =>
          Action {
            Ok(if (page == 2)
              empty
            else if (repoName == repoName1) {
              owner mustBe owner1
              repo1Contributors
            } else if (repoName == repoName2) {
              owner mustBe owner2
              repo2Contributors
            } else {
              fail()
              JsNull
            })
          }
      })(_ mustBe Vector(ContributorInfo(name1, c1 * 2), ContributorInfo(name3, 2 * c3 + c3_1), ci2, ci4))
    }
    "empty when org not found or user not authenticated. IOW, 404" in {
      testException(NotFound, a[Gh404ResponseException])
    }
    "exception when other than 200 or 404" in {
      testException(InternalServerError, a[OtherThanGh404ErrorException])
    }
  }
}
