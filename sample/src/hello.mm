class cloud.Cloud {
    rel servers : cloud.Server
}
class cloud.Server {
    att name : String
    rel softwares : cloud.Service
}
class cloud.Service {
    att name : String
    att memory : Long
}
