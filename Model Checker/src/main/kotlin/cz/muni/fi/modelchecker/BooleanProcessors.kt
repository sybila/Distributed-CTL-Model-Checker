package cz.muni.fi.modelchecker

import cz.muni.fi.ctl.Formula


class OrProcessor<N : Node, C : ColorSet<C>>(
        val formula: Formula,
        model: Model<N, C>
) : Processor<N, C>, Model<N, C> by model
{
    override fun verify() {
        for ((node, colours) in formula[0].initialNodes()) {
            node.saveFormula(formula, colours)
        }
        for ((node, colours) in formula[1].initialNodes()) {
            node.saveFormula(formula, colours)
        }
    }
}

class AndProcessor<N : Node, C : ColorSet<C>>(
        val formula: Formula,
        model: Model<N, C>
) : Processor<N, C>, Model<N, C> by model
{
    override fun verify() {
        val fst = formula[0].initialNodes()
        val snd = formula[1].initialNodes()

        for ((node, colours) in fst) {
            val other = snd[node]
            if (other != null) {
                node.saveFormula(formula, colours intersect other)
            }
        }
    }
}

class NegationProcessor<N : Node, C : ColorSet<C>>(
        val formula: Formula,
        model: Model<N, C>
) : Processor<N, C>, Model<N, C> by model
{
    override fun verify() {
        val current = formula[0].initialNodes()
        for (node in allNodes()) {
            val negated = fullColorSet subtract (current[node] ?: emptyColorSet)
            node.saveFormula(formula, negated)
        }
    }
}