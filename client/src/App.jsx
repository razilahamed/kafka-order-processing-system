import { useEffect, useState } from "react";

import {
  AppBar,
  Toolbar,
  Typography,
  Container,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Paper,
  Box,
  Chip,
  Stack,
  Divider,
  IconButton,
} from "@mui/material";

import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import RefreshIcon from "@mui/icons-material/Refresh";

const METRICS_URL = "http://localhost:4000/metrics";
const DLQ_URL = "http://localhost:4000/dlq";

function App() {
  const [metrics, setMetrics] = useState(null);
  const [dlqEvents, setDlqEvents] = useState([]);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);

  // Extracted fetch so it can be triggered manually (refresh) or by the poll.
  async function fetchData() {
    try {
      const [metricsRes, dlqRes] = await Promise.all([
        fetch(METRICS_URL),
        fetch(DLQ_URL),
      ]);

      if (!metricsRes.ok || !dlqRes.ok) {
        throw new Error("Backend returned an error status");
      }

      const metricsJson = await metricsRes.json();
      const dlqJson = await dlqRes.json();

      setMetrics(metricsJson);
      setDlqEvents(dlqJson);
      setError("");
      setLastUpdated(new Date().toISOString());
    } catch (err) {
      console.error("Error fetching data", err);
      setError(
        "Unable to fetch data from backend. Check if backend is running."
      );
    }
  }

  useEffect(() => {
    fetchData();
    const id = setInterval(fetchData, 1000); // poll every second
    return () => clearInterval(id);
  }, []);

  const handleRefresh = () => fetchData();

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top AppBar */}
      <AppBar position="static" elevation={1}>
        <Toolbar className="flex justify-between">
          <Typography variant="h6" component="div">
            Kafka Orders Dashboard
          </Typography>
          <div className="flex items-center gap-2">
            <Chip
              label="Node.js · Kafka · Avro"
              color="primary"
              variant="filled"
              size="small"
            />
            <IconButton
              size="small"
              color="inherit"
              onClick={handleRefresh}
              aria-label="refresh"
              title="Refresh now"
            >
              <RefreshIcon fontSize="small" />
            </IconButton>
            {lastUpdated && (
              <Typography variant="caption" color="inherit" className="ml-2">
                Updated: {new Date(lastUpdated).toLocaleTimeString()}
              </Typography>
            )}
          </div>
        </Toolbar>
      </AppBar>

      {/* Main Content */}
      <Container maxWidth="lg" className="py-8 flex-1">
        {error && (
          <Box className="mb-4">
            <Paper className="p-3 bg-red-900/40 border border-red-500/40">
              <Typography variant="body2" color="error">
                {error}
              </Typography>
            </Paper>
          </Box>
        )}

        {!metrics ? (
          <Box className="flex justify-center items-center h-64">
            <Typography variant="h6" color="text.secondary">
              Waiting for metrics...
            </Typography>
          </Box>
        ) : (
          <Stack spacing={4}>
            {/* Summary Cards */}
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card className="bg-slate-900/70 border border-slate-700">
                  <CardContent>
                    <div className="flex items-center justify-between">
                      <div>
                        <Typography
                          variant="overline"
                          color="text.secondary"
                          gutterBottom
                        >
                          Total Orders
                        </Typography>
                        <Typography variant="h3">
                          {metrics.totalCount}
                        </Typography>
                      </div>
                      <div className="rounded-full bg-white/10 p-3">
                        <ShoppingCartIcon
                          className="text-white"
                          fontSize="large"
                        />
                      </div>
                    </div>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      className="mt-2"
                    >
                      Total number of orders successfully processed by the
                      consumer.
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item xs={12} md={6}>
                <Card className="bg-slate-900/70 border border-slate-700">
                  <CardContent>
                    <div className="flex items-center justify-between">
                      <div>
                        <Typography
                          variant="overline"
                          color="text.secondary"
                          gutterBottom
                        >
                          Overall Average Price
                        </Typography>
                        <Typography variant="h3">
                          {metrics.overallAveragePrice.toFixed(2)}
                        </Typography>
                      </div>
                      <div className="rounded-full bg-white/10 p-3">
                        <TrendingUpIcon
                          className="text-white"
                          fontSize="large"
                        />
                      </div>
                    </div>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      className="mt-2"
                    >
                      Running average of order price across all processed
                      orders.
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {/* Per-Product Table */}
            <Paper className="bg-slate-900/60 border border-slate-700 overflow-hidden">
              <Box className="p-4">
                <Typography variant="h6" className="mb-1">
                  Per-Product Averages
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Live statistics of order counts and average prices per
                  product.
                </Typography>
              </Box>
              <Divider />
              <Box className="overflow-x-auto">
                {Object.keys(metrics.perProduct).length === 0 ? (
                  <Box className="p-4">
                    <Typography variant="body2" color="text.secondary">
                      No product data available yet.
                    </Typography>
                  </Box>
                ) : (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Product</TableCell>
                        <TableCell align="right">Count</TableCell>
                        <TableCell align="right">Average Price</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {Object.entries(metrics.perProduct).map(
                        ([product, stats]) => (
                          <TableRow key={product}>
                            <TableCell>{product}</TableCell>
                            <TableCell align="right">{stats.count}</TableCell>
                            <TableCell align="right">
                              {stats.averagePrice.toFixed(2)}
                            </TableCell>
                          </TableRow>
                        )
                      )}
                    </TableBody>
                  </Table>
                )}
              </Box>
            </Paper>

            {/* DLQ Events */}
            <Paper className="bg-slate-900/60 border border-slate-700 overflow-hidden">
              <Box className="p-4 flex items-center gap-2">
                <ErrorOutlineIcon className="text-amber-400" />
                <Box>
                  <Typography variant="h6" className="leading-tight">
                    Recent DLQ Events
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Messages that eventually failed processing and were sent to
                    the Dead Letter Queue.
                  </Typography>
                </Box>
              </Box>
              <Divider />
              <Box className="overflow-x-auto">
                {dlqEvents.length === 0 ? (
                  <Box className="p-4">
                    <Typography variant="body2" color="text.secondary">
                      No DLQ events recorded.
                    </Typography>
                  </Box>
                ) : (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Time</TableCell>
                        <TableCell>Order ID</TableCell>
                        <TableCell>Reason</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {dlqEvents.map((e, idx) => (
                        <TableRow key={idx}>
                          <TableCell>
                            {new Date(e.timestamp).toLocaleTimeString()}
                          </TableCell>
                          <TableCell>{e.orderId || "-"}</TableCell>
                          <TableCell>{e.reason}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>
            </Paper>
          </Stack>
        )}
      </Container>

      {/* Small footer */}
      <Box className="py-4 text-center text-xs text-slate-500">
        Kafka Orders · Event-Driven Demo
      </Box>
    </div>
  );
}

export default App;
