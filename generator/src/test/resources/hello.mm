enum hello.SmartMeterState {
    InError, Healthy, Pending
}

class sample.Cloud {
    rel nodes : sample.Node
}
class sample.Node {
    att name : String
    rel softwares : sample.Software
}
class sample.Software {
    att name : String
    att size : Integer
}
