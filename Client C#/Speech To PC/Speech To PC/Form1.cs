using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Threading;
using System.Net;
using System.Diagnostics;

using System.Reactive.Linq;

using Nmqtt;


namespace Speech_To_PC
{
    public partial class Form1 : Form
    {
        private static Socket clientSocket;
        public event EventHandler<DataReceivedEventArgs> DataReceived;

        public Form1()
        {
            InitializeComponent();

            //Thread t = new Thread(new ThreadStart(openPort));
            //t.Name = "Stuf";
            //t.Start();

            // This used to be in its own method and was a little more complex
            MqttHandler.Instance.Connect("q.mq.tt", 1883);

            MqttHandler.Instance.ClientMessageArrived += HandleMqttHandlerInstanceClientMessageArrived;

            MqttHandler.Instance.Subscribe("114042449736049687152/#", 0);
        }

        void HandleMqttHandlerInstanceClientMessageArrived(object sender, Nmqtt.MqttMessageEventArgs e)
        {
            // Assume the arrived message is a byte array that contains a simple ASCII string.
            var messagePublished = Encoding.ASCII.GetString((byte[])e.Message);
            //MessageHistory += String.Format("{0}: {1}{2}", e.Topic, messagePublished, Environment.NewLine);
            Console.WriteLine(messagePublished);
            if (messagePublished.Contains("serial")) {
                String[] messageHalves = messagePublished.Split(':');
                String messageHalf = messageHalves[1];
                messageHalf = messageHalf.Substring(1, messageHalf.Length - 3);
                SendKeys.SendWait(messageHalf);
            }
        }

        private void btnWrite_Click(object sender, EventArgs e)
        {
            System.Threading.Thread.Sleep(2000);
            SendKeys.SendWait("Mary had a little lamb");
        }

        /*
        private void openPort()
        {
            //IPAddress localaddr = IPAddress.Parse("192.168.0.101"); // 127.0.0.1
            IPAddress localaddr = IPAddress.Parse("127.0.0.1");
            TcpListener l = new TcpListener(localaddr, 2323);
            clientSocket = null;
            // Start waiting for a connection
            l.Start();
            while (true)
            {
                DoBeginAcceptSocket(l);
                // This thread will hang here until a connection is made by a client
                // Once a connection is made, start listening for the client
                byte[] bytes = new byte[512];
                    try
                    {
                        // Receives data from a bound Socket.  
                        int bytesRec = clientSocket.Receive(bytes);

                        // Converts byte array to string  
                        String theMessageToReceive = Encoding.ASCII.GetString(bytes, 0, bytesRec);

                        // Continues to read the data till data isn't available  
                        while (clientSocket.Available > 0)
                        {
                            bytesRec = clientSocket.Receive(bytes);
                            theMessageToReceive += Encoding.ASCII.GetString(bytes, 0, bytesRec);
                        }

                        Console.Write(theMessageToReceive);
                        SendKeys.SendWait(theMessageToReceive);
                        clientSocket.Send(Encoding.ASCII.GetBytes("Received!"));
                    }
                    catch (Exception exc)
                    {
                        MessageBox.Show(exc.ToString());
                    }
            }
        }

        public static ManualResetEvent clientConnected = new ManualResetEvent(false);

        // Accept one client connection asynchronously. 
        public static void DoBeginAcceptSocket(TcpListener listener)
        {
            // Set the event to nonsignaled state.
            clientConnected.Reset();

            // Start to listen for connections from a client.
            Console.WriteLine("Waiting for a connection...");

            // Accept the connection.  
            // BeginAcceptSocket() creates the accepted socket.
            listener.BeginAcceptSocket(
                new AsyncCallback(DoAcceptSocketCallback), listener);
            // Wait until a connection is made and processed before  
            // continuing.
            clientConnected.WaitOne();
        }

        // Process the client connection. 
        public static void DoAcceptSocketCallback(IAsyncResult ar) 
        {
            // Get the listener that handles the client request.
            TcpListener listener = (TcpListener) ar.AsyncState;

            // End the operation and display the received data on the 
            //console.
            clientSocket = listener.EndAcceptSocket(ar);

            // Process the connection here. (Add the client to a  
            // server table, read data, etc.)
            Console.WriteLine("Client connected completed");

            // Signal the calling thread to continue.
            clientConnected.Set();
        }
     */
    }

    public class TopicSubscribedEventArgs : EventArgs
    {
        public string Topic { get; private set; }

        public TopicSubscribedEventArgs(string topic)
        {
            Topic = topic;
        }
    }

    public class MqttHandler : IDisposable
    {
        private static MqttHandler instance = new MqttHandler();

        /// <summary>
        /// Stores the underlying core disposables for the topic
        /// </summary>
        private readonly IDictionary<string, IDisposable> topicSubscriptions = new Dictionary<string, IDisposable>(); 

        /// <summary>
        /// The instance of the underlying MqttClient that is connected to the server.
        /// </summary>
		private MqttClient client;

        /// <summary>
        /// Synchronization context that the mqtthandler uses to invoke the message arrived events on the same thread that connected.
        /// </summary>
        private SynchronizationContext syncContext;

        private MqttHandler()
        {
        }

        /// <summary>
        /// The instance of the MqttHandler that manages to the Mqtt connection.
        /// </summary>
        public static MqttHandler Instance
        {
            get { return instance; }
        }

        /// <summary>
        /// Connects to the specified mqtt server.
        /// </summary>
        /// <param name="server"></param>
        /// <param name="port"></param>
        /// <returns>The state of the connection.</returns>
		public Nmqtt.ConnectionState Connect (string server, short port)
		{
            client = new MqttClient(server, port, "nMqtt_Utility");
            syncContext = SynchronizationContext.Current;

			Trace.WriteLine ("Connecting to " + server + ":" + port.ToString ());

            return client.Connect("114042449736049687152", "7ca3f7c3-0d48-4d8e-a453-5924eb68e686");
		}

        /// <summary>
        /// Disconnects from the Mqtt server.
        /// </summary>
		public void Disconnect()
		{
			client.Dispose();
		}

        /// <summary>
        /// Subscribes to the specified topic.
        /// </summary>
        /// <param name="topic">The topic.</param>
        /// <param name="qos">The qos.</param>
		public void Subscribe(string topic, byte qos)
		{
            if (client == null) throw new InvalidOperationException("You must connect before you can subscribe to a topic.");

			var sub = client.ListenTo(topic, (MqttQos)qos)
                            .ObserveOn(SynchronizationContext.Current)
                            .Subscribe(msg => ClientMessageArrived(instance, new MqttMessageEventArgs(msg.Topic, msg.Payload)));

            topicSubscriptions.Add(topic, sub);
            Trace.WriteLine(String.Format("Subscribed to Topic '{0}'.", topic));
            if (TopicSubscribed != null)
            {
                syncContext.Post((data) => this.TopicSubscribed(instance, new TopicSubscribedEventArgs(topic)), null);
            }
		}

        /// <summary>
        /// Publish message to the specified topic.
        /// </summary>
        /// <param name="topic">The topic.</param>
        /// <param name="qos">The qos.</param>
        /// <param name="data">The message.</param>
        public void Publish(string topic, byte qos, string data)
        {
            if (client == null) throw new InvalidOperationException("You must connect before you can subscribe to a topic.");

            client.PublishMessage<string, AsciiPayloadConverter>(topic, (MqttQos)qos, data);
        }
        /// <summary>
        /// Publish message to the specified topic.
        /// </summary>
        /// <param name="topic">The topic.</param>
        /// <param name="qos">The qos.</param>
        /// <param name="data">The message.</param>
        public void Publish(string topic, byte qos, byte[] data)
        {
            if (client == null) throw new InvalidOperationException("You must connect before you can subscribe to a topic.");

            client.PublishMessage(topic, (MqttQos)qos, data);
        }

        /// <summary>
        /// Event fired when subscribed to a new topic
        /// </summary>
        /// TODO: MqttTopicSubscribedEventArgs
        public event EventHandler<TopicSubscribedEventArgs> TopicSubscribed;

        /// <summary>
        /// Unsubscribes from the specified topic.
        /// </summary>
        /// <param name="topic">The topic to unsubscribe.</param>
        public void Unsubscribe(string topic) {
            IDisposable sub;
            if (topicSubscriptions.TryGetValue(topic, out sub)) {
                sub.Dispose();
                topicSubscriptions.Remove(topic);
            }
        }
		
		/// <summary>
		/// Event fired when a message arrives from the remote server.
		/// </summary>
		public event EventHandler<MqttMessageEventArgs> ClientMessageArrived;

	    public void Dispose() {
	        foreach (var topicSubscription in topicSubscriptions) {
	            topicSubscription.Value.Dispose();
	        }
	    }
	}

}
