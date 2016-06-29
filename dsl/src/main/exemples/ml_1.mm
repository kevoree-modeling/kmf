

index customers : Customer {
    from name
}

class Customer {
    att name : String

    index Customers { name }
    index Customers { name }

}

class smartgrid.SmartMeter {
    att activeEnergy : Double
    att reactiveEnergy : Double
    rel customer : Customer {
        with upperBound 3
    }

    index customers : Customer {
        from name
    }

    att isAnomaly : Boolean learnedUsing {
        from "this.activeEnergy"
        from "reactiveEnery"
        from "this.elements.parent"
        from "relations.parent.myAtt ^2 + relations.parent.myAtt / 2"
        using "GaussianClassifier"
    }

    global learned att polynomialAtt : Double {
        from "TITI"
        from "GIGI"
        with precision 0.1
        using "PolynomialExtrapolation"
    }

}