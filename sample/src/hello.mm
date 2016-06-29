enum hello.SmartMeterState {
    InError, Healthy, Pending
}

index clouds : cloud.Software {
    name
}

class cloud.Cloud {
    rel servers : cloud.Server
}
class cloud.Server {
    att name : String
    rel softwares : cloud.Software

    index softwares2 : cloud.Software {

    }

}
class cloud.Software {
    att name : String
    derived att load : Double {
        using "PolynomialNode"
        with precision 0.1
    }
    att loadProfile : Double {

    }
}
