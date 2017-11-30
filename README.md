**VDM-SCRAPER**

**In brief :** 
- Project made with Scala, Akka HTTP, ScalaTest, Akka HTTP TestKit 
- MongoDB as database 
- Docker, Docker-compose 
- Git for versioning 
- SBT for dependencies 
- Code on GitHub
 
How to run :

_To run tests_ 
- Launch mongo_tests.sh (just launches a mongo container on port 27018, just make it available)
- Launch tests.sh  (launches tests and remove the previously created mongo container)

_To run in real mode_
- First way : (recommended)
    - launch start.sh 
    - Construct images of app and publish its locally
    - Stop if they exist, the previously created containers of app / db 
    - Docker-compose up 
- Second way :
    - Launch api.sh 
    - Construct an image of the api 
    - Publish it locally 
    - Stop if they exist, the previously created containers of app / db 
    - Launch the api
    - Then launch scraper.sh 
    - Construct an image of the scraper 
    - Launch the scraper 
- Third way : 
    - Launch mongo.sh (just create a mongo container on port 27017)
    - Launch sbt_api.sh 
    - Launch sbt_scraper.sh 
    
    
_About the API_ : 
- The API is available on localhost:8080. 

_Endpoints (in subject file)_
- /api/posts to get all the posts (author, from and to are optional parameters which let to filter the results)
- /api/posts/id to get the post by id 
- /admin/clean which let to clean the db if you want to re-execute the scraper 

Don't hesitate to contact me if you have any problems ! 



