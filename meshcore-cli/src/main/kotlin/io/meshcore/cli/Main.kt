package io.meshcore.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.meshcore.cli.commands.InitCommand
import io.meshcore.cli.commands.PeersCommand
import io.meshcore.cli.commands.SendCommand
import io.meshcore.cli.commands.ServeCommand
import io.meshcore.cli.commands.StatusCommand

class MeshCoreCommand : CliktCommand(
    name = "meshcore",
    help = "MeshCore – Offline-first decentralized communication platform"
) {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    MeshCoreCommand()
        .subcommands(
            InitCommand(),
            StatusCommand(),
            PeersCommand(),
            SendCommand(),
            ServeCommand()
        )
        .main(args)
}
