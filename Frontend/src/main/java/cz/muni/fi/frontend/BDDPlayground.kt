package cz.muni.fi.frontend

import cz.muni.fi.ctl.FormulaNormalizer
import cz.muni.fi.ctl.FormulaParser
import cz.muni.fi.ctl.formula.Formula
import cz.muni.fi.ctl.formula.proposition.Contradiction
import cz.muni.fi.ctl.formula.proposition.FloatProposition
import cz.muni.fi.ctl.formula.proposition.Tautology
import cz.muni.fi.modelchecker.ModelAdapter
import cz.muni.fi.modelchecker.ModelChecker
import cz.muni.fi.modelchecker.StateSpacePartitioner
import cz.muni.fi.modelchecker.mpi.tasks.BlockingTaskMessenger
import cz.muni.fi.modelchecker.mpi.tasks.OnTaskListener
import cz.muni.fi.modelchecker.mpi.termination.MPITokenMessenger
import cz.muni.fi.thomas.BDDColorSet
import cz.muni.fi.thomas.LevelNode
import cz.muni.fi.thomas.NativeModel
import cz.muni.fi.thomas.NetworkModel
import mpi.MPI
import net.sf.javabdd.BDD
import net.sf.javabdd.BDDDomain
import net.sf.javabdd.JFactory
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File
import java.util.*

fun main(args: Array<String>) {

    NativeUtils.loadLibrary("Thomas")

    //prepare benchmark
    val start = System.currentTimeMillis()

    //start MPI
    MPI.Init(args);
    if (MPI.COMM_WORLD.Rank() == 0) {
        System.out.println("MPI started on "+MPI.COMM_WORLD.Size()+" machines.");
    }

    //read and normalize formula
    val parser = FormulaParser()
    val normalizer = FormulaNormalizer()
    val formulaOrig = parser.parse(File(args[args.size - 1]));
    val formula = normalizer.normalize(formulaOrig);
    if (MPI.COMM_WORLD.Rank() == 0) {
        System.out.println("Formula prepared for verification: "+formula);
    }

    val bdd = JFactory.init(1000, 1000)
/*
    val doms = bdd.extDomain(intArrayOf(8, 16, 32))

    println(Arrays.toString(doms[0].vars()))
    println(Arrays.toString(doms[1].vars()))
    println(Arrays.toString(doms[2].vars()))
    val a = doms[0].ithVar(1).or(doms[0].ithVar(5))
    val b = doms[1].ithVar(2)
    val c = doms[2].varRange(4, 10)

    println(a.allsat())
    println(b.allsat())
    println(c.allsat())

    println(a.and(b).and(c).allsat())*/

    val model = NativeModel(args[args.size - 2])
    val partitioner = object : StateSpacePartitioner<LevelNode> {
        override fun getNodeOwner(node: LevelNode): Int = 0
        override fun getMyId(): Int = 0
    }
    val storage = NetworkModel(partitioner)

    model.loadModel(storage)

    println(model.specieContextTargetMapping)

    val domainSizes = model.specieContextTargetMapping.map {
        Pair(it.key, it.value.values.first().size)
    }

    println(domainSizes)


    val factory = JFactory.init(1000, 1000)
    val domains = factory.extDomain(domainSizes.map { it.second }.toIntArray())
    val domMap = domainSizes.map { it.first }.zip(domains).toMap()

    val one = BDDColorSet(domains.fold(domains.first().domain()) { a, b -> a.or(b.domain()) })
    val zero = BDDColorSet(domains[0].ithVar(0).and(domains[0].ithVar(0).not()))

    println("One $one")
    println("Zero $zero")

    val species = model.specieContextTargetMapping.keys.toList()
    println("Species $species")

    val newTargetMapping: List<Pair<String, Map<String, BDD>>> = model.specieContextTargetMapping.map { species ->
        Pair(species.key, species.value.map {
            Pair(it.key, toBDD(domMap[species.key]!!, it.value, bdd.zero()))
        }.toMap())
    }

    println(newTargetMapping)

    val terminators = cz.muni.fi.modelchecker.mpi.termination.Terminator.TerminatorFactory(MPITokenMessenger(MPI.COMM_WORLD))

    val taskMessenger = object : BlockingTaskMessenger<LevelNode, BDDColorSet>() {
        override fun blockingReceiveTask(taskListener: OnTaskListener<LevelNode, BDDColorSet>): Boolean {
            return false
        }
        override fun finishSelf() { }
        override fun sendTask(destinationProcess: Int, internal: LevelNode, external: LevelNode, colors: BDDColorSet) {
            throw UnsupportedOperationException()
        }
    }

    val bddFactory = object : ModelAdapter<LevelNode, BDDColorSet> {

        val lock = Object()

        val revealedPropositions = HashSet<FloatProposition>()

        val nodeCache = HashMap<Int, LevelNode>()

        val borderNodes = HashMap<Int, LevelNode>()

        val validFormulas = HashMap<LevelNode, MutableMap<Formula, BDDColorSet>>()

        public fun getNode(levels: IntArray): LevelNode {
            synchronized(lock) {
                val hash = Arrays.hashCode(levels)
                if (nodeCache.containsKey(hash)) {
                    return nodeCache[hash]!!
                } else if (borderNodes.containsKey(hash)) {
                    return borderNodes[hash]!!
                } else {
                    val n = LevelNode(levels);  //We don't have other processes
                    nodeCache.put(hash, n);
                    validFormulas[n] = HashMap()
                    return n;
                }
            }
        }

        override fun predecessorsFor(@NotNull to: LevelNode, @Nullable borders: BDDColorSet?): MutableMap<LevelNode, BDDColorSet> {
            return HashMap(distanceOne(to).mapIndexed { specie, context ->
                if (to.levels[specie] == 1) {
                    Pair(context, BDDColorSet((newTargetMapping[specie].second[context.toContext(species)]) ?: bdd.zero()))
                } else {
                    Pair(context, BDDColorSet((newTargetMapping[specie].second[context.toContext(species)] ?: bdd.zero()).not()))
                }
            }.map {
                it.second.intersect(borders)
                it
            }.toMap())
        }

        override fun successorsFor(from: LevelNode, borders: BDDColorSet?): MutableMap<LevelNode, BDDColorSet> {
            return HashMap(distanceOne(from).mapIndexed { specie, context ->
                if (context.levels[specie] == 1) {
                    Pair(context, BDDColorSet((newTargetMapping[specie].second[from.toContext(species)]) ?: bdd.zero()))
                } else {
                    Pair(context, BDDColorSet((newTargetMapping[specie].second[from.toContext(species)] ?: bdd.zero()).not()))
                }
            }.map { it.second.intersect(borders); it }.toMap())
        }

        override fun initialNodes(formula: Formula): MutableMap<LevelNode, BDDColorSet> {
            synchronized(lock) {
                if (formula is Tautology) {
                    val results = HashMap<LevelNode, BDDColorSet>()
                    for (node in nodeCache.values) {
                        results.put(node, BDDColorSet(one))
                    }
                    return results
                }
                if (formula is Contradiction) {
                    return HashMap()
                }
                if (formula is FloatProposition && !revealedPropositions.contains(formula)) {
                    revealProposition(formula)
                }
                val results = HashMap<LevelNode, BDDColorSet>()
                for (n in nodeCache.values) {
                    val validColors = validFormulas[n]!![formula]
                    if (validColors != null && !validColors.isEmpty) {
                        results.put(n, BDDColorSet(validColors))
                    }
                }
                return results
            }
        }

        override fun invertNodeSet(nodes: MutableMap<LevelNode, BDDColorSet>): MutableMap<LevelNode, BDDColorSet> {
            val results = HashMap<LevelNode, BDDColorSet>()
            for (n in nodeCache.values) {
                val full = BDDColorSet(one)
                val anti = nodes.get(n)
                if (anti != null) {
                    full.subtract(anti)
                }
                if (!full.isEmpty()) {
                    results.put(n, full)
                }
            }
            return results;
        }

        override fun addFormula(node: LevelNode, formula: Formula, parameters: BDDColorSet): Boolean {
            synchronized(lock) {
                val previous = (validFormulas[node]!![formula] ?: BDDColorSet(bdd.zero()))
                val union = BDDColorSet(previous)
                val changed = union.union(parameters)
                (validFormulas[node]!!)[formula] = union
                return changed
            }
        }

        override fun validColorsFor(node: LevelNode, formula: Formula): BDDColorSet {
            synchronized(lock) {
                if (formula is Tautology) return BDDColorSet(one)
                if (formula is Contradiction) return BDDColorSet(bdd.zero())
                if (formula is FloatProposition && !revealedPropositions.contains(formula)) {
                    revealProposition(formula)
                }
                val colorSet = validFormulas[node]!![formula]
                if (colorSet == null) {
                    return BDDColorSet(bdd.zero())
                } else {
                    return BDDColorSet(colorSet)
                }
            }
        }

        override fun purge(formula: Formula?) {

        }

        private fun revealProposition(proposition: FloatProposition) {
            for (entry in nodeCache.values) {
                if (proposition.evaluate(entry.getLevel(species.indexOf(proposition.variable)).toDouble())) {
                    (validFormulas[entry]!!)[proposition] = BDDColorSet(one)
                }
            }
            revealedPropositions.add(proposition)
        }

        private fun distanceOne(center: LevelNode): List<LevelNode> {
            return (0..(center.levels.size - 1)).map { i ->
                val newLevels = Arrays.copyOf(center.levels, center.levels.size)
                if (newLevels[i] == 0) newLevels[i] = 1
                else newLevels[i] = 0
                newLevels
            }.map { getNode(it) }
        }
    }

    fun enumSpecies(i:Int, levels: IntArray) {
        if (i < 0) {
           // println("Get node ${Arrays.toString(levels)}")
            bddFactory.getNode(levels)
        } else {
            levels[i] = 0
            enumSpecies(i-1, levels)
            levels[i] = 1
            enumSpecies(i-1, levels)
        }
    }

    enumSpecies(species.size - 1, (1..species.size).map { 0 }.toIntArray())
    println("Node count: ${bddFactory.nodeCache.size}")

    val modelChecker = ModelChecker<LevelNode, BDDColorSet>(bddFactory, partitioner, taskMessenger, terminators)
    modelChecker.verify(formula)

    //print results
    println(" ---------  RESULTS --------- ")
    if (args.size >= 3 && args[args.size - 3] == "--all") {
        for (node in bddFactory.nodeCache.values) {
            println(node.toString())
        }
    } else if (args.size >= 3 && args[args.size - 3] != "--none") {
        for (node in bddFactory.nodeCache.values) {
            val colorSet = bddFactory.validColorsFor(node, formula)
            if (!colorSet.isEmpty) {
                println(Arrays.toString(node.levels) + " " + colorSet)
                //bdd.save(Arrays.toString(node.levels), colorSet.bdd)
            }
        }
    }

    MPI.Finalize()
    System.err.println("${MPI.COMM_WORLD.Rank()} Duration: ${(System.currentTimeMillis() - start)}")
    System.exit(0)

}

fun LevelNode.toContext(species: List<String>): String {
    return this.levels.zip(species).map { "${it.second}:${it.first}" }.joinToString(separator = ",")
}

fun toBDD(dom: BDDDomain, values: List<Byte>, zero: BDD): BDD {

    val bdds = values.mapIndexed { i, byte ->
        if (byte.toInt() != 0) {
            dom.ithVar(i.toLong())
        } else null
    }.filterNotNull()

    if (bdds.isEmpty()) {
        return zero
    }

    val b = bdds.fold(bdds.first()) { a, b ->
        a.or(b)
    }
    return b
}
