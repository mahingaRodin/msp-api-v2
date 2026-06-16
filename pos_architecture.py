# =========================================================
# INSTALL FIRST
# =========================================================
# pip install diagrams graphviz
#
# ALSO INSTALL GRAPHVIZ SYSTEM PACKAGE:
#
# WINDOWS:
# https://graphviz.org/download/
#
# AFTER INSTALL:
# Add Graphviz bin folder to PATH
# Example:
# C:\Program Files\Graphviz\bin
#
# THEN RESTART TERMINAL
# =========================================================

from diagrams import Diagram, Cluster, Edge
from diagrams.onprem.client import Users
from diagrams.onprem.database import PostgreSQL
from diagrams.onprem.inmemory import Redis
from diagrams.onprem.queue import RabbitMQ
from diagrams.onprem.network import Nginx
from diagrams.onprem.compute import Server
from diagrams.programming.framework import Spring

from diagrams.aws.compute import ECS, Lambda
from diagrams.aws.database import RDS
from diagrams.aws.network import ELB
from diagrams.aws.storage import S3
from diagrams.aws.integration import SQS
from diagrams.aws.security import Cognito
from diagrams.aws.management import Cloudwatch

# =========================================================
# GLOBAL GRAPH STYLING
# =========================================================

graph_attr = {
    "fontsize": "24",
    "bgcolor": "#020617",
    "pad": "0.8",
    "splines": "spline",
    "nodesep": "0.65",
    "ranksep": "1.1",
    "fontname": "Segoe UI",
    "fontcolor": "white",
    "labelloc": "t",
    "labeljust": "c",
}

node_attr = {
    "fontname": "Segoe UI",
    "fontsize": "12",
    "fontcolor": "white",
    "style": "filled,rounded",
    "shape": "box",
    "margin": "0.35",
    "pad": "0.4",
    "color": "#1e293b",
}

edge_attr = {
    "color": "#94a3b8",
    "penwidth": "2.2",
    "fontname": "Segoe UI",
    "fontsize": "10",
    "fontcolor": "#cbd5e1",
}

# =========================================================
# MAIN DIAGRAM
# =========================================================

with Diagram(
        name="🚀 Multi-Tenant SaaS POS Architecture",
        filename="linkedin_pos_architecture",
        direction="LR",
        outformat="png",
        show=True,
        graph_attr=graph_attr,
        node_attr=node_attr,
        edge_attr=edge_attr,
):

    # =====================================================
    # USERS
    # =====================================================

    users = Users("👥\nRetail Users")

    # =====================================================
    # EDGE LAYER
    # =====================================================

    with Cluster("🌐 Edge Layer"):

        nginx = Nginx("Nginx API Gateway\nIngress Controller")

    # =====================================================
    # CORE BACKEND
    # =====================================================

    with Cluster("⚙️ Core Backend • Spring Boot Microservices"):

        auth = Spring("🔐\nAuth Service\nJWT + RBAC")

        store = Spring("🏬\nStore Service")

        inventory = Spring("📦\nInventory Service")

        order = Spring("🧾\nOrder Service")

        report = Spring("📊\nShift Reporting")

        payment = Spring("💳\nPayment Service")

    # =====================================================
    # DATA LAYER
    # =====================================================

    with Cluster("🗄️ Data Layer"):

        db = PostgreSQL(
            "🐘 PostgreSQL\nMulti-Tenant DB"
        )

        cache = Redis(
            "⚡ Redis Cache"
        )

    # =====================================================
    # ASYNC PROCESSING
    # =====================================================

    with Cluster("📨 Async Processing"):

        queue = RabbitMQ(
            "🐇 RabbitMQ\nEvents + Queues"
        )

    # =====================================================
    # PAYMENTS
    # =====================================================

    with Cluster("💸 External Payment Providers"):

        stripe = Server("💳 Stripe")

        razorpay = Server("🏦 Razorpay")

        paypack = Server("📱 Paypack")

    # =====================================================
    # AWS CLOUD
    # =====================================================

    with Cluster("☁️ AWS Cloud Migration Layer"):

        alb = ELB(
            "⚖️ Application\nLoad Balancer"
        )

        ecs = ECS(
            "☸️ ECS / EKS\nContainers"
        )

        rds = RDS(
            "🗄️ Amazon RDS"
        )

        s3 = S3(
            "🪣 S3 Storage"
        )

        sqs = SQS(
            "⚙️ SQS Queue"
        )

        lambda_fn = Lambda(
            "λ Lambda Jobs"
        )

        cognito = Cognito(
            "🛡️ Cognito Auth"
        )

        cloudwatch = Cloudwatch(
            "📈 Monitoring"
        )

    # =====================================================
    # MAIN USER FLOW
    # =====================================================

    users >> Edge(
        color="#38bdf8",
        penwidth="3.0"
    ) >> nginx

    nginx >> Edge(
        color="#22c55e",
        penwidth="2.5"
    ) >> auth

    auth >> store

    store >> inventory

    store >> order

    order >> payment

    order >> report

    # =====================================================
    # DATABASE CONNECTIONS
    # =====================================================

    auth >> Edge(
        color="#f59e0b"
    ) >> db

    store >> Edge(
        color="#f59e0b"
    ) >> db

    inventory >> Edge(
        color="#f59e0b"
    ) >> db

    order >> Edge(
        color="#f59e0b"
    ) >> db

    report >> Edge(
        color="#f59e0b"
    ) >> db

    # =====================================================
    # REDIS CACHE
    # =====================================================

    store >> Edge(
        color="#eab308"
    ) >> cache

    inventory >> Edge(
        color="#eab308"
    ) >> cache

    # =====================================================
    # ASYNC EVENTS
    # =====================================================

    order >> Edge(
        label="events",
        color="#f97316",
        style="dashed"
    ) >> queue

    queue >> Edge(
        color="#f97316",
        style="dashed"
    ) >> report

    # =====================================================
    # PAYMENTS
    # =====================================================

    payment >> Edge(
        color="#10b981"
    ) >> stripe

    payment >> Edge(
        color="#10b981"
    ) >> razorpay

    payment >> Edge(
        color="#10b981"
    ) >> paypack

    # =====================================================
    # AWS MIGRATION FLOW
    # =====================================================

    nginx >> Edge(
        color="#ff9900",
        style="bold",
        penwidth="3.0",
        label="future cloud migration"
    ) >> alb

    alb >> ecs

    ecs >> rds

    ecs >> s3

    ecs >> sqs >> lambda_fn

    ecs >> cognito

    ecs >> cloudwatch

print("✅ High-end LinkedIn architecture diagram generated.")